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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
}
