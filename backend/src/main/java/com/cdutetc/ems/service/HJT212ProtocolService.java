package com.cdutetc.ems.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * HJ/T212-2005 协议服务
 * 用于山东协议数据上报
 *
 * 协议规范：环境污染源自动监控信息传输、交换技术规范（HJ/T212-2005）
 */
@Service
@Slf4j
public class HJT212ProtocolService {

    /**
     * 构建实时数据上报包（命令 3051）
     * 按照放射源监控设备与平台通信协议文档（2019.9.6）要求
     *
     * @param mn      设备编号
     * @param password 访问密码
     * @param data    数据字段
     * @return HJ/T212 数据包
     */
    public String buildRealtimeDataPacket(String mn, String password, HJT212Data data) {
        try {
            // 1. 构建数据段（包含所有设备配置字段）
            String dataSegment = buildDataSegment(mn, data);

            // 2. 构建完整包（按照HJ/T212-2005协议标准格式）
            // ST=61: 现场机编号（放射源监控设备）
            // CN=3051: 实时数据上报命令
            String packet = String.format("QN=%s;ST=61;CN=3051;PW=%s;CP=&&%s&&",
                    generateQN(),
                    password,
                    dataSegment);

            // 3. 计算数据段长度（4位十进制）
            // 修正：协议要求包含数据段长度字段
            String dataLength = String.format("%04d", packet.length());

            // 4. 计算 CRC 校验（对数据段进行校验）
            String crc = calculateCRC16(packet);

            // 5. 构建完整数据包：## + 长度 + 数据段 + CRC + \r\n
            // 修正：添加了缺失的数据段长度字段
            String result = "##" + dataLength + packet + crc + "\r\n";

            log.debug("📦 构建HJ/T212实时数据包: MN={}, DataTime={}, 长度={}",
                    mn, data.getDataTime(), result.length());
            log.trace("数据包内容: {}", result);
            return result;

        } catch (Exception e) {
            log.error("❌ 构建HJ/T212数据包失败: {}", e.getMessage(), e);
            throw new RuntimeException("构建HJ/T212数据包失败", e);
        }
    }

    /**
     * 构建数据段
     * 按照放射源监控设备协议文档要求，包含所有必需字段
     */
    private String buildDataSegment(String mn, HJT212Data data) {
        StringJoiner sj = new StringJoiner(";");

        // 设备标识字段
        sj.add("MN=" + mn);

        if (data.getInspectionMachineNumber() != null) {
            sj.add("Ma=" + data.getInspectionMachineNumber());  // 探伤机编号6位
        }

        if (data.getSourceNumber() != null) {
            sj.add("Rno=" + data.getSourceNumber());  // 放射源编号12位
        }

        if (data.getSourceType() != null) {
            sj.add("Xtype=" + data.getSourceType());  // 放射源类型2位: 01=Ⅰ类~05=Ⅴ类
        }

        if (data.getOriginalActivity() != null) {
            sj.add("LastAct=" + data.getOriginalActivity());  // 原始活度: 2.700E004格式
        }

        if (data.getCurrentActivity() != null) {
            sj.add("NowAct=" + data.getCurrentActivity());  // 当前活度: 1.300E004格式
        }

        if (data.getSourceProductionDate() != null) {
            sj.add("SourceTime=" + data.getSourceProductionDate());  // 出厂日期: YYYYMMDD
        }

        // 实时数据字段
        sj.add("DataTime=" + data.getDataTime());

        if (data.getCpm() != null) {
            sj.add("Xvalue=" + data.getCpm());  // 剂量率
        }

        // 可选字段：阈值（如果配置了）
        if (data.getThreshold() != null) {
            sj.add("Thres=" + data.getThreshold());
        }

        // 可选字段：报警类型
        if (data.getAlertType() != null) {
            sj.add("AlertType=" + data.getAlertType());  // 01=源丢失等
        }

        if (data.getVoltage() != null) {
            sj.add("BattChar=" + data.getVoltage());  // 电源电量
        }

        if (data.getLongitude() != null && data.getLatitude() != null) {
            sj.add("LONG=" + data.getLongitude());  // GPS经度
            sj.add("LAT=" + data.getLatitude());    // GPS纬度
        }

        if (data.getGpsFlag() != null) {
            // 根据文档：A=GPS提供位置，V=基站提供位置
            String gpsFlag = (data.getGpsFlag() == 1) ? "A" : "V";
            sj.add("Sig=" + gpsFlag);
        }

        return sj.toString();
    }

    /**
     * 生成请求编号（QN）
     * 格式：YYYYMMDDHHMMSSZZZZS
     */
    private String generateQN() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String base = now.format(formatter);

        // 添加4位随机数
        int random = (int) (Math.random() * 10000);
        return String.format("%s%04d1", base, random);
    }

    /**
     * 计算 CRC16 校验码
     *
     * @param data 数据（不含包头包尾）
     * @return CRC16 校验码（4位Hex）
     */
    private String calculateCRC16(String data) {
        byte[] bytes = data.getBytes(StandardCharsets.US_ASCII);
        int crc = 0xFFFF;

        for (byte b : bytes) {
            crc ^= (b & 0xFF);

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }

        return String.format("%04X", crc);
    }

    /**
     * 解析应答包
     * 支持标准文本格式和新服务器的二进制格式
     *
     * @param response 应答包
     * @return 是否成功
     */
    public boolean parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        try {
            // 尝试解析二进制格式（新服务器）
            // 格式：CM + 状态码(1B) + 其他数据(6B)
            if (response.length() >= 9 && response.startsWith("CM")) {
                // 检查是否是二进制响应
                byte[] bytes = response.getBytes(StandardCharsets.ISO_8859_1);

                // 检查魔术字节 CM (0x434D)
                if (bytes.length >= 3 && bytes[0] == 0x43 && bytes[1] == 0x4D) {
                    int statusCode = bytes[2] & 0xFF; // 转换为无符号整数

                    // 0x01 = 成功，0x8D = 失败，0x03 = 初始状态
                    boolean success = (statusCode == 0x01);
                    log.debug("📥 二进制响应: Magic=CM, Status=0x{}, 成功={}",
                            String.format("%02X", statusCode), success);

                    return success;
                }
            }

            // 解析标准文本格式（旧服务器）
            // 去除包头包尾
            String content = response.replace("##", "").replace("\r\n", "");

            // 检查执行结果标志
            if (content.contains("QN=") && content.contains("ST=")) {
                // 解析 ST（执行结果）字段
                String[] parts = content.split(";");
                for (String part : parts) {
                    if (part.startsWith("ST=")) {
                        String st = part.substring(3);
                        // ST=91 表示成功，ST=92 表示失败
                        boolean success = "91".equals(st);
                        log.debug("📥 文本响应: ST={}, 成功={}", st, success);
                        return success;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ 解析HJ/T212应答失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析二进制应答包（新服务器专用）
     * 格式：CM (2B) + Status (1B) + Data/Padding (6B)
     *
     * @param responseBytes 应答包字节数组
     * @return 是否成功
     */
    public boolean parseBinaryResponse(byte[] responseBytes) {
        if (responseBytes == null || responseBytes.length < 3) {
            return false;
        }

        try {
            // 检查魔术字节 CM (0x434D)
            if (responseBytes[0] == 0x43 && responseBytes[1] == 0x4D) {
                int statusCode = responseBytes[2] & 0xFF;

                // 状态码含义
                // 0x01 = 数据上传成功
                // 0x03 = 初始连接状态
                // 0x8D = 数据上传失败
                boolean success = (statusCode == 0x01);

                log.debug("📥 二进制响应解析: Status=0x{}, 成功={}, 长度={}",
                        String.format("%02X", statusCode), success, responseBytes.length);

                return success;
            }

            log.warn("⚠️ 无效的二进制响应: Magic={}",
                    String.format("%02X%02X", responseBytes[0], responseBytes[1]));
            return false;

        } catch (Exception e) {
            log.warn("⚠️ 解析二进制响应失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * HJ/T212 数据对象（放射源监控设备协议）
     * 包含设备配置字段和实时监测字段
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HJT212Data {
        // 设备标识字段
        /**
         * 探伤机编号（Ma）6位
         */
        private String inspectionMachineNumber;

        /**
         * 放射源编号（Rno）12位
         */
        private String sourceNumber;

        /**
         * 放射源类型（Xtype）2位
         * 01=Ⅰ类, 02=Ⅱ类, 03=Ⅲ类, 04=Ⅳ类, 05=Ⅴ类
         */
        private String sourceType;

        /**
         * 放射源原始活度（LastAct）9位
         * 格式: 2.700E004
         */
        private String originalActivity;

        /**
         * 放射源当前活度（NowAct）9位
         * 格式: 1.300E004
         */
        private String currentActivity;

        /**
         * 放射源出厂日期（SourceTime）8位
         * 格式: YYYYMMDD
         */
        private String sourceProductionDate;

        // 实时监测数据字段
        /**
         * 数据时间（DataTime）14位
         * 格式: YYYYMMDDHHmmss
         */
        private String dataTime;

        /**
         * 剂量率（Xvalue）10位
         */
        private Double cpm;

        /**
         * 阈值（Thres）可选
         */
        private Double threshold;

        /**
         * 报警类型（AlertType）2位可选
         * 01=源丢失, 02=计数阻塞, 03=欠压报警, 04=低计数, 05=通信故障
         */
        private String alertType;

        /**
         * 电源电量（BattChar）6位
         */
        private Double voltage;

        /**
         * GPS经度（LONG）10位
         */
        private String longitude;

        /**
         * GPS纬度（LAT）9位
         */
        private String latitude;

        /**
         * GPS标志（Sig）1位
         * 0/A=GPS提供位置, 1/V=基站提供位置
         * 注意：当前使用0/1，构建时转换为A/V
         */
        private Integer gpsFlag;
    }
}
