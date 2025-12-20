package com.ems.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ems.service.mqtt.UnifiedMQTTDataProcessor.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 实时缓存服务
 * 统一管理Redis缓存，确保数据一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis键前缀
    private static final String REALTIME_PREFIX = "ems:realtime:";
    private static final String DEVICE_STATUS_PREFIX = "ems:device:status:";

    // TTL配置
    private static final Duration REALTIME_TTL = Duration.ofMinutes(30);    // 实时数据30分钟

    /**
     * 更新实时数据缓存
     * 确保Redis和MySQL存储相同的数据结构
     */
    public void updateRealtimeData(DeviceData data, LocalDateTime timestamp) {
        try {
            String deviceId = data.getDeviceId();
            long timestampMs = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            // 统一数据结构
            Map<String, Object> unifiedData = new HashMap<>();
            unifiedData.put("deviceId", deviceId);
            unifiedData.put("cpmValue", data.getCpmValue());
            unifiedData.put("batteryVoltage", data.getBatteryVoltage());
            unifiedData.put("triggerType", data.getTriggerType());
            unifiedData.put("transmissionWay", data.getTransmissionWay());
            unifiedData.put("multiFlag", data.getMultiFlag());
            unifiedData.put("messageType", data.getMessageType());
            unifiedData.put("sourceFlag", data.getSourceFlag());
            unifiedData.put("localTimeString", data.getLocalTimeString());
            unifiedData.put("timestamp", timestamp.toString());
            unifiedData.put("timestampMs", timestampMs);

            // GPS位置信息
            if (data.getBdsLongitude() != null) {
                Map<String, Object> bdsLocation = new HashMap<>();
                bdsLocation.put("longitude", data.getBdsLongitude());
                bdsLocation.put("latitude", data.getBdsLatitude());
                bdsLocation.put("utc", data.getBdsUtc());
                bdsLocation.put("useful", data.getBdsUseful());
                unifiedData.put("bdsLocation", bdsLocation);
            }

            if (data.getLbsLongitude() != null) {
                Map<String, Object> lbsLocation = new HashMap<>();
                lbsLocation.put("longitude", data.getLbsLongitude());
                lbsLocation.put("latitude", data.getLbsLatitude());
                lbsLocation.put("useful", data.getLbsUseful());
                unifiedData.put("lbsLocation", lbsLocation);
            }

            // 存储统一数据
            String key = REALTIME_PREFIX + deviceId + ":data";
            redisTemplate.opsForValue().set(key, unifiedData, REALTIME_TTL);

            // 存储设备状态
            updateDeviceStatus(deviceId, "ONLINE", timestamp);

            // 存储单独的指标数据（便于快速查询）
            if (data.getCpmValue() != null) {
                redisTemplate.opsForValue().set(
                    REALTIME_PREFIX + deviceId + ":cpm",
                    createMetricValue(data.getCpmValue(), timestampMs),
                    REALTIME_TTL
                );
            }

            if (data.getBatteryVoltage() != null) {
                redisTemplate.opsForValue().set(
                    REALTIME_PREFIX + deviceId + ":battery",
                    createMetricValue(data.getBatteryVoltage(), timestampMs),
                    REALTIME_TTL
                );
            }

            log.debug("✅ 实时数据缓存更新: 设备={}, CPM={}, 电池={}mV",
                    deviceId, data.getCpmValue(), data.getBatteryVoltage());

        } catch (Exception e) {
            log.error("❌ 实时数据缓存更新失败: 设备={}", data.getDeviceId(), e);
        }
    }

    /**
     * 更新设备状态
     */
    public void updateDeviceStatus(String deviceId, String status, LocalDateTime timestamp) {
        try {
            String key = DEVICE_STATUS_PREFIX + deviceId;
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", status);
            statusData.put("timestamp", timestamp.toString());
            statusData.put("timestampMs", timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());

            redisTemplate.opsForValue().set(key, statusData, Duration.ofHours(2));
        } catch (Exception e) {
            log.error("❌ 设备状态缓存更新失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 获取实时数据
     */
    public Map<String, Object> getRealtimeData(String deviceId) {
        try {
            String key = REALTIME_PREFIX + deviceId + ":data";
            return (Map<String, Object>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("❌ 实时数据获取失败: 设备={}", deviceId, e);
            return null;
        }
    }

    /**
     * 获取设备状态
     */
    public String getDeviceStatus(String deviceId) {
        try {
            String key = DEVICE_STATUS_PREFIX + deviceId;
            Map<String, Object> statusData = (Map<String, Object>) redisTemplate.opsForValue().get(key);
            return statusData != null ? (String) statusData.get("status") : null;
        } catch (Exception e) {
            log.error("❌ 设备状态获取失败: 设备={}", deviceId, e);
            return null;
        }
    }

    /**
     * 获取指标值
     */
    public MetricValue getMetricValue(String deviceId, String metricName) {
        try {
            String key = REALTIME_PREFIX + deviceId + ":" + metricName;
            return (MetricValue) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("❌ 指标值获取失败: 设备={}, 指标={}", deviceId, metricName, e);
            return null;
        }
    }

    /**
     * 获取所有在线设备
     */
    public Set<String> getOnlineDevices() {
        try {
            String pattern = DEVICE_STATUS_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            return keys.stream()
                    .map(key -> key.substring(DEVICE_STATUS_PREFIX.length()))
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("❌ 在线设备列表获取失败", e);
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 清除设备缓存
     */
    public void clearDeviceCache(String deviceId) {
        try {
            Set<String> keys = redisTemplate.keys(REALTIME_PREFIX + deviceId + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ 设备缓存已清除: 设备={}, 删除键数量={}", deviceId, keys.size());
            }
        } catch (Exception e) {
            log.error("❌ 设备缓存清除失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 创建指标值对象
     */
    private MetricValue createMetricValue(Integer value, long timestampMs) {
        MetricValue metricValue = new MetricValue();
        metricValue.setValue(value);
        metricValue.setTimestampMs(timestampMs);
        metricValue.setCacheTime(System.currentTimeMillis());
        return metricValue;
    }

    /**
     * 指标值数据结构
     */
    public static class MetricValue {
        private Integer value;
        private Long timestampMs;
        private Long cacheTime;

        // Getters and Setters
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
        public Long getTimestampMs() { return timestampMs; }
        public void setTimestampMs(Long timestampMs) { this.timestampMs = timestampMs; }
        public Long getCacheTime() { return cacheTime; }
        public void setCacheTime(Long cacheTime) { this.cacheTime = cacheTime; }
    }
}