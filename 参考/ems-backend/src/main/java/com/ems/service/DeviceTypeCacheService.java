package com.ems.service;

import com.ems.entity.DeviceType;
import com.ems.repository.DeviceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备类型缓存服务
 * 提供设备类型信息的Redis缓存管理
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTypeCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceTypeRepository deviceTypeRepository;

    // 缓存键常量
    private static final String ENABLED_TYPES_KEY = "ems:device:enabled-types";
    private static final String DEVICE_TYPE_CONFIG_KEY = "ems:device:type:%s";
    private static final String DEVICE_TO_TYPE_KEY = "ems:device:device-to-type:%s";

    // 固定TTL
    private static final Duration CACHE_TTL = Duration.ofHours(6); // 6小时

    // 本地缓存（热点数据）
    private final ConcurrentHashMap<String, DeviceType> localDeviceTypeCache = new ConcurrentHashMap<>();
    private static final int LOCAL_CACHE_SIZE = 1000;

    /**
     * 启动时预加载启用的设备类型
     */
    public void loadEnabledDeviceTypes() {
        try {
            List<DeviceType> enabledTypes = deviceTypeRepository.findByEnabledTrue();

            // 存储到Redis
            redisTemplate.opsForValue().set(ENABLED_TYPES_KEY, enabledTypes, CACHE_TTL);

            // 存储到本地缓存
            localDeviceTypeCache.clear();
            for (DeviceType type : enabledTypes) {
                localDeviceTypeCache.put(type.getTypeCode(), type);
                redisTemplate.opsForValue().set(
                    String.format(DEVICE_TYPE_CONFIG_KEY, type.getTypeCode()),
                    type,
                    CACHE_TTL
                );
            }

            log.info("✅ 加载了{}个启用的设备类型到缓存", enabledTypes.size());

        } catch (Exception e) {
            log.error("❌ 加载设备类型缓存失败", e);
        }
    }

    /**
     * 获取设备类型（优先从缓存）
     */
    public DeviceType getDeviceType(String deviceId) {
        try {
            // 1. 检查本地缓存是否有设备ID到类型的映射
            String cachedTypeCode = (String) redisTemplate.opsForValue().get(
                String.format(DEVICE_TO_TYPE_KEY, deviceId)
            );

            if (cachedTypeCode != null) {
                return getDeviceTypeByCode(cachedTypeCode);
            }

            // 2. 从数据库查找设备类型并缓存映射
            String deviceTypeCode = findDeviceTypeCodeByDeviceId(deviceId);
            if (deviceTypeCode != null) {
                // 缓存设备ID到类型的映射
                redisTemplate.opsForValue().set(
                    String.format(DEVICE_TO_TYPE_KEY, deviceId),
                    deviceTypeCode,
                    CACHE_TTL
                );

                return getDeviceTypeByCode(deviceTypeCode);
            }

            return null;

        } catch (Exception e) {
            log.error("❌ 获取设备类型失败: deviceId={}", deviceId, e);
            return null;
        }
    }

    /**
     * 根据类型代码获取设备类型
     */
    public DeviceType getDeviceTypeByCode(String typeCode) {
        try {
            // 1. 本地缓存查找
            DeviceType cachedType = localDeviceTypeCache.get(typeCode);
            if (cachedType != null) {
                return cachedType;
            }

            // 2. Redis缓存查找
            String cacheKey = String.format(DEVICE_TYPE_CONFIG_KEY, typeCode);
            DeviceType redisType = (DeviceType) redisTemplate.opsForValue().get(cacheKey);
            if (redisType != null) {
                // 加入本地缓存
                if (localDeviceTypeCache.size() < LOCAL_CACHE_SIZE) {
                    localDeviceTypeCache.put(typeCode, redisType);
                }
                return redisType;
            }

            // 3. 数据库查找并缓存
            DeviceType dbType = deviceTypeRepository.findByTypeCode(typeCode).orElse(null);
            if (dbType != null) {
                // 存储到Redis
                redisTemplate.opsForValue().set(cacheKey, dbType, CACHE_TTL);

                // 存储到本地缓存
                if (localDeviceTypeCache.size() < LOCAL_CACHE_SIZE) {
                    localDeviceTypeCache.put(typeCode, dbType);
                }
            }

            return dbType;

        } catch (Exception e) {
            log.error("❌ 获取设备类型失败: typeCode={}", typeCode, e);
            return null;
        }
    }

    /**
     * 获取所有启用的设备类型
     */
    public List<DeviceType> getEnabledDeviceTypes() {
        try {
            // 1. Redis缓存查找
            @SuppressWarnings("unchecked")
            List<DeviceType> cached = (List<DeviceType>) redisTemplate.opsForValue().get(ENABLED_TYPES_KEY);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }

            // 2. 数据库查找并缓存
            List<DeviceType> enabledTypes = deviceTypeRepository.findByEnabledTrue();
            redisTemplate.opsForValue().set(ENABLED_TYPES_KEY, enabledTypes, CACHE_TTL);

            return enabledTypes;

        } catch (Exception e) {
            log.error("❌ 获取启用设备类型失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 清除设备缓存
     */
    public void clearDeviceCache(String deviceId) {
        try {
            String cacheKey = String.format(DEVICE_TO_TYPE_KEY, deviceId);
            redisTemplate.delete(cacheKey);
            log.debug("已清除设备缓存: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("❌ 清除设备缓存失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 刷新设备类型缓存
     */
    public void refreshDeviceTypeCache() {
        try {
            // 清除Redis缓存
            redisTemplate.delete(ENABLED_TYPES_KEY);

            // 清除所有设备类型配置缓存
            Set<String> keys = redisTemplate.keys("ems:device:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除了{}个缓存键", keys.size());
            }

            // 清除本地缓存
            localDeviceTypeCache.clear();

            // 重新加载
            loadEnabledDeviceTypes();

            log.info("🔄 设备类型缓存已刷新");

        } catch (Exception e) {
            log.error("❌ 刷新设备类型缓存失败", e);
        }
    }

    /**
     * 根据设备ID查找设备类型代码
     * 这里需要与DeviceService集成，暂时使用默认逻辑
     */
    private String findDeviceTypeCodeByDeviceId(String deviceId) {
        // TODO: 与DeviceService集成，根据设备ID查询设备类型
        // 暂时根据设备ID前缀判断类型

        if (deviceId.startsWith("RAD-") || deviceId.startsWith("RADIATION-")) {
            return "RADIATION";
        } else if (deviceId.startsWith("ENV-") || deviceId.startsWith("ENVIRONMENT-")) {
            return "ENVIRONMENT";
        }

        // 默认返回辐射监测仪
        return "RADIATION";
    }

    /**
     * 预热特定设备类型的缓存
     */
    public void preloadDeviceType(String typeCode) {
        try {
            DeviceType deviceType = getDeviceTypeByCode(typeCode);
            if (deviceType != null) {
                log.debug("预加载设备类型缓存成功: typeCode={}", typeCode);
            }
        } catch (Exception e) {
            log.error("❌ 预加载设备类型缓存失败: typeCode={}", typeCode, e);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getCacheStatistics() {
        try {
            long localCacheSize = localDeviceTypeCache.size();

            // 检查Redis缓存
            boolean redisEnabledTypesExists = redisTemplate.hasKey(ENABLED_TYPES_KEY);

            return new CacheStatistics(
                localCacheSize,
                redisEnabledTypesExists ? 1 : 0,
                CACHE_TTL.toHours()
            );

        } catch (Exception e) {
            log.error("❌ 获取缓存统计信息失败", e);
            return new CacheStatistics(0, 0, 0);
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        private final long localCacheSize;
        private final long redisCacheCount;
        private final long ttlHours;

        public CacheStatistics(long localCacheSize, long redisCacheCount, long ttlHours) {
            this.localCacheSize = localCacheSize;
            this.redisCacheCount = redisCacheCount;
            this.ttlHours = ttlHours;
        }

        public long getLocalCacheSize() { return localCacheSize; }
        public long getRedisCacheCount() { return redisCacheCount; }
        public long getTtlHours() { return ttlHours; }

        @Override
        public String toString() {
            return String.format("CacheStats{local=%d, redis=%d, ttl=%dh}",
                               localCacheSize, redisCacheCount, ttlHours);
        }
    }
}