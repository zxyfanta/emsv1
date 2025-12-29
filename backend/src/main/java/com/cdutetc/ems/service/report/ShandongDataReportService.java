package com.cdutetc.ems.service.report;

import com.cdutetc.ems.config.DataReportProperties;
import com.cdutetc.ems.dto.DeviceReportConfig;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.service.HJT212ProtocolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 山东协议数据上报服务
 * TCP + HJ/T212-2005
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShandongDataReportService {

    private final DataReportProperties properties;
    private final HJT212ProtocolService protocolService;

    /**
     * 上报数据到山东监管平台
     *
     * @param config 设备上报配置
     * @param data    辐射设备数据
     */
    @Async("reportExecutor")
    public void report(DeviceReportConfig config, RadiationDeviceData data) {
        long startTime = System.currentTimeMillis();
        String deviceCode = config.getDeviceCode();

        Socket socket = null;
        try {
            log.info("📤 [山东] 开始上报: deviceCode={}", deviceCode);

            // 1. 构建 HJ/T212 数据对象
            HJT212ProtocolService.HJT212Data hjData = buildHJT212Data(config, data);

            // 2. 构建数据包
            String packet = protocolService.buildRealtimeDataPacket(
                    config.getDeviceCode(),
                    properties.getShandong().getPassword(),
                    hjData
            );

            log.debug("📦 HJ/T212数据包: {}", packet);

            // 3. 建立 TCP 连接
            socket = new Socket(
                    properties.getShandong().getHost(),
                    properties.getShandong().getPort()
            );

            socket.setSoTimeout(properties.getShandong().getSoTimeout());
            log.debug("🔌 TCP连接已建立: {}:{}",
                    properties.getShandong().getHost(),
                    properties.getShandong().getPort());

            // ⭐ 重要：接收并忽略服务器的初始CM消息（不响应握手）
            // 根据测试验证，新服务器(221.214.62.118:20050)会在连接后立即发送9字节二进制消息
            // 格式: CM (2B) + Status (1B) + Data (6B)
            // 正确做法：接收并忽略，直接发送数据包
            try {
                socket.setSoTimeout(1000); // 短超时读取初始消息
                byte[] initialBuffer = new byte[1024];
                int initialRead = socket.getInputStream().read(initialBuffer);

                if (initialRead > 0) {
                    // 提取实际接收到的字节数组
                    byte[] actualData = new byte[initialRead];
                    System.arraycopy(initialBuffer, 0, actualData, 0, initialRead);

                    String hexResponse = bytesToHex(actualData);
                    log.info("📥 [山东] 收到服务器初始消息: {} 字节, HEX={}",
                        initialRead, hexResponse);

                    // 检查是否是CM消息
                    if (actualData.length >= 2 && actualData[0] == 0x43 && actualData[1] == 0x4D) {
                        if (actualData.length >= 3) {
                            int statusCode = actualData[2] & 0xFF;
                            log.debug("📋 [山东] 初始消息解析: Magic=CM, Status=0x{}, 说明={}",
                                String.format("%02X", statusCode),
                                statusCode == 0x03 ? "初始连接状态" : "未知状态");
                        }
                        log.info("ℹ️ [山东] 策略: 忽略初始消息，不响应握手（符合协议测试结果）");
                    }
                } else {
                    log.debug("ℹ️ [山东] 无初始消息（正常情况）");
                }
            } catch (java.net.SocketTimeoutException e) {
                log.debug("ℹ️ [山东] 无初始消息（超时）");
            } finally {
                socket.setSoTimeout(properties.getShandong().getSoTimeout()); // 恢复原超时
            }

            // 4. 发送数据
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(packet);
            out.flush();

            log.debug("📤 数据已发送");

            // 5. 接收应答
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            String response = in.readLine();
            long duration = System.currentTimeMillis() - startTime;

            // ⭐ 改进：详细记录响应信息
            if (response != null) {
                byte[] responseBytes = response.getBytes(StandardCharsets.ISO_8859_1);
                String hexResponse = bytesToHex(responseBytes);
                log.info("📥 [山东] 服务器响应: length={}, hex={}, ascii={}",
                    response.length(), hexResponse,
                    response.length() < 100 ? response : response.substring(0, 100) + "...");
            } else {
                log.warn("⚠️ [山东] 服务器无响应");
            }

            // 6. 处理应答
            boolean success = protocolService.parseResponse(response);

            if (success) {
                log.info("✅ [山东] 上报成功: deviceCode={}, 耗时={}ms, 应答={}",
                        deviceCode, duration, response);
                // TODO: 更新设备上报状态
            } else {
                log.warn("⚠️ [山东] 上报失败: deviceCode={}, 耗时={}ms, 应答={}",
                        deviceCode, duration, response);
                // TODO: 记录失败日志
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [山东] 上报异常: deviceCode={}, 耗时={}ms, error={}",
                    deviceCode, duration, e.getMessage(), e);
            // TODO: 记录失败日志，支持重连
        } finally {
            // 7. 关闭连接
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    log.debug("🔌 TCP连接已关闭");
                } catch (Exception e) {
                    log.warn("⚠️ 关闭TCP连接失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 构建 HJ/T212 数据对象
     * 按照放射源监控设备协议文档要求，包含所有设备配置字段
     *
     * @param config 设备配置
     * @param data    设备数据
     * @return HJ/T212 数据对象
     */
    private HJT212ProtocolService.HJT212Data buildHJT212Data(
            DeviceReportConfig config,
            RadiationDeviceData data) {

        // GPS标志：根据gpsType判断（BDS=1, LBS=0）
        Integer gpsFlag = "BDS".equals(data.getGpsType()) ? 1 : 0;

        return HJT212ProtocolService.HJT212Data.builder()
                // 设备标识字段（设备配置静态数据）
                .inspectionMachineNumber(config.getInspectionMachineNumber())
                .sourceNumber(config.getSourceNumber())
                .sourceType(config.getSourceType())
                .originalActivity(config.getOriginalActivity())
                .currentActivity(config.getCurrentActivity())
                .sourceProductionDate(config.getSourceProductionDate())  // DeviceReportConfig已经是String格式
                // 实时监测数据字段
                .dataTime(formatDataTime(data.getRecordTime()))
                .cpm(data.getCpm())
                .voltage(data.getBatvolt())
                .gpsFlag(gpsFlag)
                .longitude(data.getGpsLongitude())  // 直接使用统一的GPS字段
                .latitude(data.getGpsLatitude())    // 直接使用统一的GPS字段
                .build();
    }

    /**
     * 格式化数据时间
     */
    private String formatDataTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 字节数组转十六进制字符串
     * 用于调试日志输出
     *
     * @param bytes 字节数组
     * @return 十六进制字符串，如 "43 4D 03 02"
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
            if (i < bytes.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
