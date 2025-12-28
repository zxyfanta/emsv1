package com.cdutetc.ems.service.report;

import com.cdutetc.ems.config.DataReportProperties;
import com.cdutetc.ems.dto.DeviceReportConfig;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.service.Sm2EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 四川协议数据上报服务
 * HTTP + SM2 加密
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SichuanDataReportService {

    private final DataReportProperties properties;
    private final RestTemplate restTemplate;
    private final Sm2EncryptionService sm2EncryptionService;
    private final ObjectMapper objectMapper;

    /**
     * 上报数据到四川监管平台
     * 按照接口文档要求构建完整的外层包装结构
     *
     * @param config 设备上报配置
     * @param data    辐射设备数据
     */
    @Async("reportExecutor")
    public void report(DeviceReportConfig config, RadiationDeviceData data) {
        long startTime = System.currentTimeMillis();
        String deviceCode = config.getDeviceCode();

        try {
            log.info("📤 [四川] 开始上报: deviceCode={}", deviceCode);

            // 1. 构建完整的外层payload结构
            Map<String, Object> payload = buildCompletePayload(config, data);

            // 2. 转换为 JSON
            String jsonData = objectMapper.writeValueAsString(payload);
            log.debug("📦 完整上报数据: {}", jsonData);

            // 3. SM2 加密（如果配置了公钥）
            String encryptedData = jsonData;
            if (properties.getSichuan().getSm2PublicKey() != null
                    && !properties.getSichuan().getSm2PublicKey().isEmpty()) {
                encryptedData = sm2EncryptionService.encrypt(
                        jsonData,
                        properties.getSichuan().getSm2PublicKey()
                );
                log.debug("🔒 数据已SM2加密");
            }

            // 4. 构建 HTTP 请求（按照文档要求）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", properties.getSichuan().getApiKey());

            // 注意：文档显示完整URL应该包含apiKey参数
            String fullUrl = properties.getSichuan().getUrl() + "?apiKey=" + properties.getSichuan().getApiKey();

            HttpEntity<String> request = new HttpEntity<>(encryptedData, headers);

            // 5. 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;

            // 6. 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [四川] 上报成功: deviceCode={}, 耗时={}ms, HTTP={}",
                        deviceCode, duration, response.getStatusCodeValue());
                // TODO: 更新设备上报状态
            } else {
                log.warn("⚠️ [四川] 上报失败: deviceCode={}, HTTP={}, 响应={}",
                        deviceCode, response.getStatusCodeValue(), response.getBody());
                // TODO: 记录失败日志
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [四川] 上报异常: deviceCode={}, 耗时={}ms, error={}",
                    deviceCode, duration, e.getMessage(), e);
            // TODO: 记录失败日志，支持重试
        }
    }

    /**
     * 构建完整的外层payload结构
     * 按照接口文档第36-47行的要求
     *
     * @param config 设备配置
     * @param data    设备数据
     * @return 完整的外层payload Map
     */
    private Map<String, Object> buildCompletePayload(DeviceReportConfig config, RadiationDeviceData data) {
        Map<String, Object> payload = new HashMap<>();

        // 外层字段
        payload.put("deviceCode", config.getDeviceCode());
        payload.put("paramType", "HOUR");  // 固定为HOUR（小时粒度）
        payload.put("dataType", "HOUR");
        payload.put("timestamp", System.currentTimeMillis());

        // 构建data数组
        Map<String, String> dataItem = new HashMap<>();
        dataItem.put("dataTime", formatDataTimeReadable(data.getRecordTime()));
        dataItem.put("dataStr", buildDataStr(config, data));

        payload.put("data", List.of(dataItem));

        // TODO: 生成签名（文档要求但未提供具体算法）
        // payload.put("signature", generateSignature(payload));

        return payload;
    }

    /**
     * 构建dataStr内容（JSON字符串）
     * 按照接口文档要求
     */
    private String buildDataStr(DeviceReportConfig config, RadiationDeviceData data) {
        Map<String, Object> dataStr = new HashMap<>();

        // 基本信息
        dataStr.put("CODE", config.getDeviceCode());
        dataStr.put("Nuclide", config.getNuclide() != null ? config.getNuclide() : "Cs-137");
        dataStr.put("GPS", determineGPS(config));

        // GPS 坐标（根据优先级选择）
        if ("BDS".equals(config.getGpsPriority()) || "BDS_THEN_LBS".equals(config.getGpsPriority())) {
            // 优先北斗
            if (data.getBdsLongitude() != null && data.getBdsLatitude() != null) {
                Map<String, String> coords = formatCoordinate(
                    Double.parseDouble(data.getBdsLongitude()),
                    Double.parseDouble(data.getBdsLatitude())
                );
                dataStr.put("LNG", coords.get("LNG"));
                dataStr.put("LAT", coords.get("LAT"));
            } else if ("BDS_THEN_LBS".equals(config.getGpsPriority())) {
                // 北斗无效，使用基站
                putLBSCoordinates(dataStr, data);
            }
        } else {
            // 优先基站
            putLBSCoordinates(dataStr, data);
        }

        // 辐射值和电压（修复格式）
        dataStr.put("FSY", data.getCpm() != null ? data.getCpm() : 0.0);
        dataStr.put("Vbat", formatVoltage(data.getBatvolt()));

        try {
            return objectMapper.writeValueAsString(dataStr);
        } catch (Exception e) {
            log.error("❌ 构建dataStr JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 添加基站坐标（用于dataStr）
     */
    private void putLBSCoordinates(Map<String, Object> dataStr, RadiationDeviceData data) {
        if (data.getLbsLongitude() != null && data.getLbsLatitude() != null) {
            Map<String, String> coords = formatCoordinate(
                Double.parseDouble(data.getLbsLongitude()),
                Double.parseDouble(data.getLbsLatitude())
            );
            dataStr.put("LNG", coords.get("LNG"));
            dataStr.put("LAT", coords.get("LAT"));
        }
    }

    /**
     * 确定 GPS 标志
     */
    private int determineGPS(DeviceReportConfig config) {
        return "BDS".equals(config.getGpsPriority()) ? 1 : 0;
    }

    /**
     * 格式化坐标为度分格式
     * 输入：度度格式（如 117.0090）
     * 输出：度分格式（如 11700.5400）
     */
    private Map<String, String> formatCoordinate(Double longitude, Double latitude) {
        Map<String, String> result = new HashMap<>();

        // 经度转换
        int lngDegree = longitude.intValue();
        double lngMinute = (longitude - lngDegree) * 60;
        String lngFormatted = String.format("%d%.4f", lngDegree, lngMinute);

        // 纬度转换
        int latDegree = latitude.intValue();
        double latMinute = (latitude - latDegree) * 60;
        String latFormatted = String.format("%d%.4f", latDegree, latMinute);

        result.put("LNG", lngFormatted);
        result.put("LAT", latFormatted);

        return result;
    }

    /**
     * 格式化电压（添加"V"单位）
     * 输入：3.8（Double）
     * 输出："3.8V"（String）
     */
    private String formatVoltage(Double voltage) {
        if (voltage == null) {
            return "0.0V";
        }
        return String.format("%.1fV", voltage);
    }

    /**
     * 格式化数据时间为可读格式（四川协议要求）
     * 输入：LocalDateTime
     * 输出："yyyy-MM-dd HH:mm:ss"
     */
    private String formatDataTimeReadable(LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
