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
     *
     * @param mn      设备编号
     * @param password 访问密码
     * @param data    数据字段
     * @return HJ/T212 数据包
     */
    public String buildRealtimeDataPacket(String mn, String password, HJT212Data data) {
        try {
            // 1. 构建数据段
            String dataSegment = buildDataSegment(mn, data);

            // 2. 构建完整包
            String packet = String.format("QN=%s;ST=21;CN=%s;PW=%s;MN=%s;Flag=8;CP=&&DataTime=%s;%s&&",
                    generateQN(),
                    data.getPolId(),
                    password,
                    mn,
                    data.getDataTime(),
                    dataSegment);

            // 3. 计算 CRC 校验
            String crc = calculateCRC16(packet);

            // 4. 添加包头包尾和校验码
            String result = "##" + packet + crc + "\r\n";

            log.debug("📦 构建HJ/T212实时数据包: MN={}, DataTime={}", mn, data.getDataTime());
            return result;

        } catch (Exception e) {
            log.error("❌ 构建HJ/T212数据包失败: {}", e.getMessage(), e);
            throw new RuntimeException("构建HJ/T212数据包失败", e);
        }
    }

    /**
     * 构建数据段
     */
    private String buildDataSegment(String mn, HJT212Data data) {
        StringJoiner sj = new StringJoiner(";");

        // 根据不同类型添加字段
        if (data.getCpm() != null) {
            sj.add("Xvalue=" + data.getCpm());
        }

        if (data.getVoltage() != null) {
            sj.add("BattChar=" + data.getVoltage());
        }

        if (data.getLongitude() != null && data.getLatitude() != null) {
            sj.add("LONG=" + data.getLongitude());
            sj.add("LAT=" + data.getLatitude());
        }

        if (data.getGpsFlag() != null) {
            sj.add("Sig=" + data.getGpsFlag());
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
     *
     * @param response 应答包
     * @return 是否成功
     */
    public boolean parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        try {
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
                        log.debug("📥 HJ/T212应答: ST={}, 成功={}", st, success);
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
     * HJ/T212 数据对象
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HJT212Data {
        /**
         * 污染物编号（PolId）
         */
        private String polId;

        /**
         * 数据时间
         */
        private String dataTime;

        /**
         * 剂量率（CPM）
         */
        private Double cpm;

        /**
         * 电源电量（电压）
         */
        private Double voltage;

        /**
         * 经度
         */
        private String longitude;

        /**
         * 纬度
         */
        private String latitude;

        /**
         * GPS标志
         * 0: 无效
         * 1: 有效
         */
        private Integer gpsFlag;
    }
}
