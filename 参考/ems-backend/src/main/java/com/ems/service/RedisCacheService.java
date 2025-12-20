package com.ems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ems.dto.common.ApiResponse;
import com.ems.repository.device.DeviceGroupMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 负责实时数据缓存、设备状态缓存和查询优化
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DeviceGroupMappingRepository mappingRepository;

    // 缓存键前缀
    private static final String SENSOR_DATA_PREFIX = "ems:sensor:data:";
    private static final String DEVICE_STATUS_PREFIX = "ems:device:status:";
    private static final String REALTIME_DATA_PREFIX = "ems:realtime:";
    private static final String AGGREGATED_DATA_PREFIX = "ems:aggregated:";

    // 缓存过期时间
    private static final Duration REALTIME_DATA_TTL = Duration.ofMinutes(30); // 30分钟
    private static final Duration DEVICE_STATUS_TTL = Duration.ofHours(2);     // 2小时
    private static final Duration AGGREGATED_DATA_TTL = Duration.ofHours(1);   // 1小时

    /**
     * 缓存实时传感器数据
     */
    public void cacheRealTimeData(String deviceId, String metricName, Object value, LocalDateTime timestamp) {
        try {
            String key = buildKey(REALTIME_DATA_PREFIX, deviceId, metricName);

            // 构建缓存数据结构
            SensorDataCache cacheData = new SensorDataCache(
                deviceId, metricName, value, timestamp, LocalDateTime.now()
            );

            String jsonData = objectMapper.writeValueAsString(cacheData);

            // 存储数据并设置过期时间
            redisTemplate.opsForValue().set(key, jsonData, REALTIME_DATA_TTL);

            log.debug("💾 实时数据已缓存: 设备={}, 指标={}, 值={}", deviceId, metricName, value);

        } catch (JsonProcessingException e) {
            log.error("❌ 实时数据缓存失败: 设备={}, 指标={}", deviceId, metricName, e);
        } catch (Exception e) {
            log.error("❌ Redis写入失败: 设备={}, 指标={}", deviceId, metricName, e);
        }
    }

    /**
     * 获取实时传感器数据
     */
    public SensorDataCache getRealTimeData(String deviceId, String metricName) {
        try {
            String key = buildKey(REALTIME_DATA_PREFIX, deviceId, metricName);
            String jsonData = (String) redisTemplate.opsForValue().get(key);

            if (jsonData == null) {
                log.debug("🔍 缓存未命中: 设备={}, 指标={}", deviceId, metricName);
                return null;
            }

            SensorDataCache cacheData = objectMapper.readValue(jsonData, SensorDataCache.class);
            log.debug("✅ 缓存命中: 设备={}, 指标={}", deviceId, metricName);

            return cacheData;

        } catch (JsonProcessingException e) {
            log.error("❌ 缓存数据解析失败: 设备={}, 指标={}", deviceId, metricName, e);
            return null;
        } catch (Exception e) {
            log.error("❌ Redis读取失败: 设备={}, 指标={}", deviceId, metricName, e);
            return null;
        }
    }

    /**
     * 获取设备所有实时数据
     */
    public List<SensorDataCache> getDeviceRealTimeData(String deviceId) {
        try {
            String pattern = buildKey(REALTIME_DATA_PREFIX, deviceId, "*");
            List<String> keys = (List<String>) redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            List<SensorDataCache> result = new ArrayList<>();

            if (values != null) {
                for (Object value : values) {
                    if (value instanceof String) {
                        try {
                            SensorDataCache cacheData = objectMapper.readValue((String) value, SensorDataCache.class);
                            result.add(cacheData);
                        } catch (JsonProcessingException e) {
                            log.error("❌ 缓存数据解析失败", e);
                        }
                    }
                }
            }

            log.debug("✅ 获取设备实时数据: 设备={}, 返回{}条", deviceId, result.size());
            return result;

        } catch (Exception e) {
            log.error("❌ 获取设备实时数据失败: 设备={}", deviceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 缓存设备状态
     */
    public void cacheDeviceStatus(String deviceId, String status) {
        try {
            String key = buildKey(DEVICE_STATUS_PREFIX, deviceId);

            DeviceStatusCache cacheData = new DeviceStatusCache(
                deviceId, status, LocalDateTime.now()
            );

            String jsonData = objectMapper.writeValueAsString(cacheData);
            redisTemplate.opsForValue().set(key, jsonData, DEVICE_STATUS_TTL);

            log.debug("💾 设备状态已缓存: 设备={}, 状态={}", deviceId, status);

        } catch (JsonProcessingException e) {
            log.error("❌ 设备状态缓存失败: 设备={}", deviceId, e);
        } catch (Exception e) {
            log.error("❌ Redis写入失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 获取设备状态
     */
    public String getDeviceStatus(String deviceId) {
        try {
            String key = buildKey(DEVICE_STATUS_PREFIX, deviceId);
            String jsonData = (String) redisTemplate.opsForValue().get(key);

            if (jsonData == null) {
                return null;
            }

            DeviceStatusCache cacheData = objectMapper.readValue(jsonData, DeviceStatusCache.class);
            return cacheData.getStatus();

        } catch (JsonProcessingException e) {
            log.error("❌ 设备状态解析失败: 设备={}", deviceId, e);
            return null;
        } catch (Exception e) {
            log.error("❌ Redis读取失败: 设备={}", deviceId, e);
            return null;
        }
    }

    /**
     * 缓存聚合数据
     */
    public void cacheAggregatedData(String deviceId, String metricName, String aggregationLevel,
                                   Object aggregatedValue, LocalDateTime aggregationTime) {
        try {
            String key = buildKey(AGGREGATED_DATA_PREFIX, deviceId, metricName, aggregationLevel);

            AggregatedDataCache cacheData = new AggregatedDataCache(
                deviceId, metricName, aggregationLevel, aggregatedValue, aggregationTime, LocalDateTime.now()
            );

            String jsonData = objectMapper.writeValueAsString(cacheData);
            redisTemplate.opsForValue().set(key, jsonData, AGGREGATED_DATA_TTL);

            log.debug("💾 聚合数据已缓存: 设备={}, 指标={}, 级别={}", deviceId, metricName, aggregationLevel);

        } catch (JsonProcessingException e) {
            log.error("❌ 聚合数据缓存失败: 设备={}, 指标={}, 级别={}", deviceId, metricName, aggregationLevel, e);
        } catch (Exception e) {
            log.error("❌ Redis写入失败: 设备={}, 指标={}, 级别={}", deviceId, metricName, aggregationLevel, e);
        }
    }

    /**
     * 获取聚合数据
     */
    public AggregatedDataCache getAggregatedData(String deviceId, String metricName, String aggregationLevel) {
        try {
            String key = buildKey(AGGREGATED_DATA_PREFIX, deviceId, metricName, aggregationLevel);
            String jsonData = (String) redisTemplate.opsForValue().get(key);

            if (jsonData == null) {
                return null;
            }

            return objectMapper.readValue(jsonData, AggregatedDataCache.class);

        } catch (JsonProcessingException e) {
            log.error("❌ 聚合数据解析失败: 设备={}, 指标={}, 级别={}", deviceId, metricName, aggregationLevel, e);
            return null;
        } catch (Exception e) {
            log.error("❌ Redis读取失败: 设备={}, 指标={}, 级别={}", deviceId, metricName, aggregationLevel, e);
            return null;
        }
    }

    /**
     * 清理设备相关的缓存数据
     */
    public void clearDeviceCache(String deviceId) {
        try {
            // 清理实时数据缓存
            String realtimePattern = buildKey(REALTIME_DATA_PREFIX, deviceId, "*");
            List<String> realtimeKeys = (List<String>) redisTemplate.keys(realtimePattern);
            if (realtimeKeys != null && !realtimeKeys.isEmpty()) {
                redisTemplate.delete(realtimeKeys);
                log.info("🗑️ 已清理设备实时数据缓存: 设备={}, 删除{}个键", deviceId, realtimeKeys.size());
            }

            // 清理设备状态缓存
            String statusKey = buildKey(DEVICE_STATUS_PREFIX, deviceId);
            redisTemplate.delete(statusKey);
            log.info("🗑️ 已清理设备状态缓存: 设备={}", deviceId);

            // 清理聚合数据缓存
            String aggregatedPattern = buildKey(AGGREGATED_DATA_PREFIX, deviceId, "*");
            List<String> aggregatedKeys = (List<String>) redisTemplate.keys(aggregatedPattern);
            if (aggregatedKeys != null && !aggregatedKeys.isEmpty()) {
                redisTemplate.delete(aggregatedKeys);
                log.info("🗑️ 已清理设备聚合数据缓存: 设备={}, 删除{}个键", deviceId, aggregatedKeys.size());
            }

        } catch (Exception e) {
            log.error("❌ 清理设备缓存失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 清理过期数据（通过TTL自动清理，此方法用于手动触发）
     */
    public void evictExpiredData() {
        log.info("🧹 Redis数据清理由TTL自动管理，无需手动清理");
    }

    /**
     * 检查Redis连接状态
     */
    public boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().get("test:connection");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构建缓存键
     */
    private String buildKey(String prefix, String... parts) {
        return prefix + String.join(":", parts);
    }

    /**
     * 传感器数据缓存对象
     */
    public static class SensorDataCache {
        private String deviceId;
        private String metricName;
        private Object value;
        private LocalDateTime timestamp;
        private LocalDateTime cacheTime;

        // 构造函数
        public SensorDataCache() {}

        public SensorDataCache(String deviceId, String metricName, Object value,
                             LocalDateTime timestamp, LocalDateTime cacheTime) {
            this.deviceId = deviceId;
            this.metricName = metricName;
            this.value = value;
            this.timestamp = timestamp;
            this.cacheTime = cacheTime;
        }

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public LocalDateTime getCacheTime() { return cacheTime; }
        public void setCacheTime(LocalDateTime cacheTime) { this.cacheTime = cacheTime; }
    }

    /**
     * 设备状态缓存对象
     */
    public static class DeviceStatusCache {
        private String deviceId;
        private String status;
        private LocalDateTime cacheTime;

        // 位置相关信息
        private Double currentLongitude;
        private Double currentLatitude;
        private String lastLocationType; // "BDS", "LBS"
        private LocalDateTime lastUpdateTime;

        // 构造函数
        public DeviceStatusCache() {}

        public DeviceStatusCache(String deviceId, String status, LocalDateTime cacheTime) {
            this.deviceId = deviceId;
            this.status = status;
            this.cacheTime = cacheTime;
        }

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCacheTime() { return cacheTime; }
        public void setCacheTime(LocalDateTime cacheTime) { this.cacheTime = cacheTime; }

        public Double getCurrentLongitude() { return currentLongitude; }
        public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }
        public Double getCurrentLatitude() { return currentLatitude; }
        public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }
        public String getLastLocationType() { return lastLocationType; }
        public void setLastLocationType(String lastLocationType) { this.lastLocationType = lastLocationType; }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }

    /**
     * 聚合数据缓存对象
     */
    public static class AggregatedDataCache {
        private String deviceId;
        private String metricName;
        private String aggregationLevel;
        private Object aggregatedValue;
        private LocalDateTime aggregationTime;
        private LocalDateTime cacheTime;

        // 构造函数
        public AggregatedDataCache() {}

        public AggregatedDataCache(String deviceId, String metricName, String aggregationLevel,
                                 Object aggregatedValue, LocalDateTime aggregationTime, LocalDateTime cacheTime) {
            this.deviceId = deviceId;
            this.metricName = metricName;
            this.aggregationLevel = aggregationLevel;
            this.aggregatedValue = aggregatedValue;
            this.aggregationTime = aggregationTime;
            this.cacheTime = cacheTime;
        }

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public String getAggregationLevel() { return aggregationLevel; }
        public void setAggregationLevel(String aggregationLevel) { this.aggregationLevel = aggregationLevel; }
        public Object getAggregatedValue() { return aggregatedValue; }
        public void setAggregatedValue(Object aggregatedValue) { this.aggregatedValue = aggregatedValue; }
        public LocalDateTime getAggregationTime() { return aggregationTime; }
        public void setAggregationTime(LocalDateTime aggregationTime) { this.aggregationTime = aggregationTime; }
        public LocalDateTime getCacheTime() { return cacheTime; }
        public void setCacheTime(LocalDateTime cacheTime) { this.cacheTime = cacheTime; }
    }

    // ==================== 新增方法：支持位置状态和实时数据缓存 ====================

    /**
     * 更新设备位置状态缓存
     */
    public void updateDeviceLocationStatus(String deviceId, Double longitude, Double latitude,
                                          String locationType, LocalDateTime recordTime) {
        try {
            String key = buildKey(DEVICE_STATUS_PREFIX, deviceId);

            // 获取现有状态数据
            String jsonData = (String) redisTemplate.opsForValue().get(key);
            DeviceStatusCache cacheData;

            if (jsonData != null) {
                cacheData = objectMapper.readValue(jsonData, DeviceStatusCache.class);
            } else {
                cacheData = new DeviceStatusCache(deviceId, "ONLINE", recordTime);
            }

            // 更新位置信息
            cacheData.setCurrentLongitude(longitude);
            cacheData.setCurrentLatitude(latitude);
            cacheData.setLastLocationType(locationType);
            cacheData.setLastUpdateTime(recordTime);

            jsonData = objectMapper.writeValueAsString(cacheData);
            redisTemplate.opsForValue().set(key, jsonData, DEVICE_STATUS_TTL);

            log.debug("设备位置状态缓存更新成功: 设备={}, 位置=({},{})", deviceId, longitude, latitude);

        } catch (Exception e) {
            log.error("更新设备位置状态缓存失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 保存5分钟实时CPM数据
     */
    public void saveRealtimeCpmData(String deviceId, Integer cpmValue, LocalDateTime recordTime) {
        try {
            String key = buildKey("realtime:cpm", deviceId);

            // 创建数据点
            RealtimeDataPoint dataPoint = new RealtimeDataPoint(
                recordTime, cpmValue.doubleValue()
            );

            // 使用Redis List存储最近5分钟的数据
            String pointJson = objectMapper.writeValueAsString(dataPoint);
            redisTemplate.opsForList().rightPush(key, pointJson);

            // 设置过期时间（6分钟，比数据保留时间稍长）
            redisTemplate.expire(key, Duration.ofMinutes(6));

            // 清理过期数据（保留5分钟内的）
            cleanExpiredRealtimeData(key, Duration.ofMinutes(5));

            log.debug("实时CPM数据保存成功: 设备={}, 值={}", deviceId, cpmValue);

        } catch (Exception e) {
            log.error("保存实时CPM数据失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 保存5分钟实时电池数据
     */
    public void saveRealtimeBatteryData(String deviceId, Double batteryVoltage, LocalDateTime recordTime) {
        try {
            String key = buildKey("realtime:battery", deviceId);

            // 创建数据点
            RealtimeDataPoint dataPoint = new RealtimeDataPoint(
                recordTime, batteryVoltage
            );

            // 使用Redis List存储最近5分钟的数据
            String pointJson = objectMapper.writeValueAsString(dataPoint);
            redisTemplate.opsForList().rightPush(key, pointJson);

            // 设置过期时间（6分钟，比数据保留时间稍长）
            redisTemplate.expire(key, Duration.ofMinutes(6));

            // 清理过期数据（保留5分钟内的）
            cleanExpiredRealtimeData(key, Duration.ofMinutes(5));

            log.debug("实时电池数据保存成功: 设备={}, 值={}V", deviceId, batteryVoltage);

        } catch (Exception e) {
            log.error("保存实时电池数据失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 获取设备5分钟实时CPM数据
     */
    public List<RealtimeDataPoint> getRealtimeCpmData(String deviceId) {
        try {
            String key = buildKey("realtime:cpm", deviceId);
            return getRealtimeDataFromList(key);

        } catch (Exception e) {
            log.error("获取实时CPM数据失败: 设备={}", deviceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取设备5分钟实时电池数据
     */
    public List<RealtimeDataPoint> getRealtimeBatteryData(String deviceId) {
        try {
            String key = buildKey("realtime:battery", deviceId);
            return getRealtimeDataFromList(key);

        } catch (Exception e) {
            log.error("获取实时电池数据失败: 设备={}", deviceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从Redis List获取实时数据
     */
    @SuppressWarnings("unchecked")
    private List<RealtimeDataPoint> getRealtimeDataFromList(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) {
                return new ArrayList<>();
            }

            List<Object> objects = redisTemplate.opsForList().range(key, 0, -1);
            List<String> jsonList = objects.stream()
                    .map(obj -> obj.toString())
                    .collect(java.util.stream.Collectors.toList());
            List<RealtimeDataPoint> dataPoints = new ArrayList<>();

            for (String json : jsonList) {
                try {
                    RealtimeDataPoint point = objectMapper.readValue(json, RealtimeDataPoint.class);
                    dataPoints.add(point);
                } catch (Exception e) {
                    log.warn("解析实时数据点失败: {}", json);
                }
            }

            return dataPoints;

        } catch (Exception e) {
            log.error("获取实时数据失败: key={}", key, e);
            return new ArrayList<>();
        }
    }

    /**
     * 清理过期的实时数据
     */
    @SuppressWarnings("unchecked")
    private void cleanExpiredRealtimeData(String key, Duration retentionPeriod) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minus(retentionPeriod);

            // 获取所有数据点
            List<Object> objects = redisTemplate.opsForList().range(key, 0, -1);
            List<String> allPoints = objects.stream()
                    .map(Object::toString)
                    .toList();
            if (allPoints == null || allPoints.isEmpty()) {
                return;
            }

            // 找到要保留的第一个数据点
            int firstToKeep = 0;
            for (int i = 0; i < allPoints.size(); i++) {
                try {
                    RealtimeDataPoint point = objectMapper.readValue(allPoints.get(i), RealtimeDataPoint.class);
                    if (point.getTimestamp().isAfter(cutoffTime)) {
                        firstToKeep = i;
                        break;
                    }
                } catch (Exception e) {
                    log.warn("解析数据点失败，跳过: {}", allPoints.get(i));
                }
            }

            // 删除过期数据
            if (firstToKeep > 0) {
                redisTemplate.opsForList().trim(key, firstToKeep, -1);
                log.debug("清理过期实时数据: key={}, 删除数量={}", key, firstToKeep);
            }

        } catch (Exception e) {
            log.error("清理过期实时数据失败: key={}", key, e);
        }
    }

    /**
     * 实时数据点内部类
     */
    public static class RealtimeDataPoint {
        private LocalDateTime timestamp;
        private Double value;

        public RealtimeDataPoint() {}

        public RealtimeDataPoint(LocalDateTime timestamp, Double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
    }

    // ===== 设备分组缓存同步方法 =====

    /**
     * 同步更新设备分组变更后的缓存
     * 当设备被添加到分组或从分组中移除时调用
     */
    public void syncDeviceGroupChange(Long deviceId, Long groupId, boolean isAdded) {
        try {
            log.debug("同步设备分组缓存: deviceId={}, groupId={}, operation={}",
                    deviceId, groupId, isAdded ? "ADD" : "REMOVE");

            // 清理与设备状态相关的缓存，因为分组变更可能影响状态查询
            clearDeviceCache(deviceId.toString());

            // 清理实时数据缓存以确保数据一致性
            String realtimePattern = buildKey(REALTIME_DATA_PREFIX, "*", "*");
            List<String> realtimeKeys = (List<String>) redisTemplate.keys(realtimePattern);
            if (realtimeKeys != null && !realtimeKeys.isEmpty()) {
                // 只清理该设备的实时数据
                String deviceRealtimePattern = buildKey(REALTIME_DATA_PREFIX, deviceId.toString(), "*");
                List<String> deviceRealtimeKeys = (List<String>) redisTemplate.keys(deviceRealtimePattern);
                if (deviceRealtimeKeys != null && !deviceRealtimeKeys.isEmpty()) {
                    redisTemplate.delete(deviceRealtimeKeys);
                    log.debug("清理设备实时数据缓存: deviceId={}, 数量={}", deviceId, deviceRealtimeKeys.size());
                }
            }

        } catch (Exception e) {
            log.error("同步设备分组缓存失败: deviceId={}, groupId={}", deviceId, groupId, e);
        }
    }

    /**
     * 批量同步设备分组变更
     */
    public void syncBatchDeviceGroupChange(Long groupId, List<Long> deviceIds, boolean isAdded) {
        try {
            log.debug("批量同步设备分组缓存: groupId={}, deviceIds={}, operation={}",
                    groupId, deviceIds.size(), isAdded ? "ADD" : "REMOVE");

            for (Long deviceId : deviceIds) {
                syncDeviceGroupChange(deviceId, groupId, isAdded);
            }

        } catch (Exception e) {
            log.error("批量同步设备分组缓存失败: groupId={}", groupId, e);
        }
    }

    /**
     * 同步分组删除后的缓存清理
     */
    public void syncGroupDeletion(Long groupId) {
        try {
            log.debug("同步分组删除缓存清理: groupId={}", groupId);

            // 获取该分组中的所有设备
            List<Long> deviceIds = mappingRepository.findDeviceIdsByGroupId(groupId);

            // 清理这些设备的相关缓存
            for (Long deviceId : deviceIds) {
                clearDeviceCache(deviceId.toString());
            }

        } catch (Exception e) {
            log.error("同步分组删除缓存失败: groupId={}", groupId, e);
        }
    }

    /**
     * 确保缓存一致性：验证缓存与数据库的一致性
     */
    public boolean verifyCacheConsistency(Long deviceId, Long groupId) {
        try {
            // 检查缓存中的设备状态
            String cachedStatus = getDeviceStatus(deviceId.toString());
            if (cachedStatus == null) {
                // 缓存中没有状态，可能是正常的
                return true;
            }

            // 检查数据库中的映射关系
            boolean existsInDb = mappingRepository.findByDeviceIdAndGroupId(deviceId, groupId).isPresent();

            // 如果数据库中存在映射关系但缓存状态为离线，可能需要刷新
            if (existsInDb && "OFFLINE".equals(cachedStatus)) {
                log.debug("检测到缓存不一致: deviceId={}, groupId= {}, 需要刷新缓存", deviceId, groupId);
                clearDeviceCache(deviceId.toString());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("验证缓存一致性失败: deviceId={}, groupId={}", deviceId, groupId, e);
            return false;
        }
    }
}