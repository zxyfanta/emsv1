package com.ems.service.mqtt;

import com.ems.entity.DeviceType;
import com.ems.service.DeviceTypeCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备类型路由器
 * 根据设备ID和MQTT主题自动路由消息到对应的处理器
 *
 * @author EMS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTypeRouter {

    private final DeviceTypeCacheService deviceTypeCacheService;

    // 设备类型处理器映射
    private final Map<String, DeviceDataProcessor> processors = new ConcurrentHashMap<>();

    // 热点设备类型本地缓存
    private final Map<String, DeviceType> hotDeviceTypeCache = new ConcurrentHashMap<>();
    private static final int HOT_CACHE_SIZE = 1000;

    /**
     * 注册设备类型处理器
     */
    public void registerProcessor(String deviceTypeCode, DeviceDataProcessor processor) {
        processors.put(deviceTypeCode, processor);
        log.info("注册设备类型处理器: typeCode={}, processor={}", deviceTypeCode, processor.getClass().getSimpleName());
    }

    /**
     * 根据设备ID获取设备类型（优先使用缓存）
     */
    public DeviceType getDeviceType(String deviceId) {
        // 1. 热点设备本地缓存查找
        DeviceType hotType = hotDeviceTypeCache.get(deviceId);
        if (hotType != null) {
            return hotType;
        }

        // 2. 缓存服务查找
        DeviceType type = deviceTypeCacheService.getDeviceType(deviceId);
        if (type != null) {
            // 加入热点缓存
            if (hotDeviceTypeCache.size() >= HOT_CACHE_SIZE) {
                // 简单的LRU：清除最老的元素
                String firstKey = hotDeviceTypeCache.keySet().iterator().next();
                hotDeviceTypeCache.remove(firstKey);
            }
            hotDeviceTypeCache.put(deviceId, type);
        }

        return type;
    }

    /**
     * 根据MQTT主题查找设备类型
     */
    public DeviceType getDeviceTypeByTopic(String topic) {
        try {
            // 首先尝试从缓存获取启用的设备类型
            for (DeviceType deviceType : deviceTypeCacheService.getEnabledDeviceTypes()) {
                if (matchesTopicPattern(topic, deviceType.getMqttTopicPattern())) {
                    return deviceType;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("根据主题查找设备类型失败: topic={}", topic, e);
            return null;
        }
    }

    /**
     * 判断设备类型是否启用
     */
    public boolean isDeviceTypeEnabled(String deviceId) {
        DeviceType type = getDeviceType(deviceId);
        return type != null && type.getEnabled();
    }

    /**
     * 获取对应的设备数据处理器
     */
    public DeviceDataProcessor getProcessor(String deviceTypeCode) {
        return processors.get(deviceTypeCode);
    }

    /**
     * 获取对应的设备数据处理器（根据设备ID）
     */
    public DeviceDataProcessor getProcessorByDeviceId(String deviceId) {
        DeviceType deviceType = getDeviceType(deviceId);
        if (deviceType != null) {
            return getProcessor(deviceType.getTypeCode());
        }
        return null;
    }

    /**
     * 路由MQTT消息到对应的处理器
     */
    public void routeMessage(String deviceId, String topic, String payload) {
        try {
            // 1. 确定设备类型
            DeviceType deviceType = getDeviceType(deviceId);
            if (deviceType == null) {
                log.warn("未找到设备类型: deviceId={}, topic={}", deviceId, topic);
                return;
            }

            // 2. 检查是否启用
            if (!deviceType.getEnabled()) {
                log.debug("设备类型已禁用: deviceId={}, type={}", deviceId, deviceType.getTypeCode());
                return;
            }

            // 3. 获取处理器
            DeviceDataProcessor processor = getProcessor(deviceType.getTypeCode());
            if (processor == null) {
                log.warn("未找到设备类型处理器: type={}", deviceType.getTypeCode());
                return;
            }

            // 4. 路由消息
            processor.processMessage(deviceId, topic, payload);

            log.debug("消息路由成功: deviceId={}, type={}, processor={}, topic={}",
                     deviceId, deviceType.getTypeCode(), processor.getClass().getSimpleName(), topic);

        } catch (Exception e) {
            log.error("消息路由失败: deviceId={}, topic={}", deviceId, topic, e);
        }
    }

    /**
     * 🆕 使用方案一主题结构路由MQTT消息
     * 直接从主题解析设备类型，无需查询数据库
     */
    public void routeMessageWithDeviceType(String deviceId, String deviceTypeCode, String topic, String payload) {
        try {
            log.debug("🆕 使用新主题结构路由消息: deviceId={}, deviceTypeCode={}, topic={}",
                     deviceId, deviceTypeCode, topic);

            // 1. 验证主题格式
            if (!DeviceType.isValidTopicFormat(topic)) {
                log.warn("⚠️ 无效的主题格式: topic={}", topic);
                return;
            }

            // 2. 获取设备类型（从缓存服务根据代码获取）
            DeviceType deviceType = deviceTypeCacheService.getDeviceTypeByCode(deviceTypeCode);
            if (deviceType == null) {
                log.warn("未找到设备类型: deviceTypeCode={}, topic={}", deviceTypeCode, topic);
                return;
            }

            // 3. 检查是否启用
            if (!deviceType.getEnabled()) {
                log.debug("设备类型已禁用: deviceId={}, deviceTypeCode={}", deviceId, deviceTypeCode);
                return;
            }

            // 4. 获取处理器
            DeviceDataProcessor processor = getProcessor(deviceTypeCode);
            if (processor == null) {
                log.warn("未找到设备类型处理器: deviceTypeCode={}", deviceTypeCode);
                return;
            }

            // 5. 路由消息
            processor.processMessage(deviceId, topic, payload);

            log.debug("✅ 新主题结构消息路由成功: deviceId={}, deviceTypeCode={}, processor={}, topic={}",
                     deviceId, deviceTypeCode, processor.getClass().getSimpleName(), topic);

        } catch (Exception e) {
            log.error("❌ 新主题结构消息路由失败: deviceId={}, deviceTypeCode={}, topic={}",
                     deviceId, deviceTypeCode, topic, e);
        }
    }

    /**
     * 验证设备ID格式
     */
    public boolean validateDeviceIdFormat(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }

        // 检查设备ID是否符合命名规范
        return deviceId.startsWith("RAD-") || deviceId.startsWith("ENV-") ||
               deviceId.startsWith("RADIATION-") || deviceId.startsWith("ENVIRONMENT-");
    }

    /**
     * 根据设备ID推断设备类型
     */
    public DeviceType inferDeviceTypeFromDeviceId(String deviceId) {
        if (deviceId.startsWith("RAD-") || deviceId.startsWith("RADIATION-")) {
            return deviceTypeCacheService.getDeviceTypeByCode("RADIATION");
        } else if (deviceId.startsWith("ENV-") || deviceId.startsWith("ENVIRONMENT-")) {
            return deviceTypeCacheService.getDeviceTypeByCode("ENVIRONMENT");
        }

        // 默认返回辐射监测仪
        return deviceTypeCacheService.getDeviceTypeByCode("RADIATION");
    }

    /**
     * 清除热点设备缓存
     */
    public void clearHotCache(String deviceId) {
        hotDeviceTypeCache.remove(deviceId);
    }

    /**
     * 清除所有热点缓存
     */
    public void clearAllHotCache() {
        hotDeviceTypeCache.clear();
    }

    /**
     * 获取路由统计信息
     */
    public RoutingStatistics getStatistics() {
        return new RoutingStatistics(
            hotDeviceTypeCache.size(),
            processors.size(),
            deviceTypeCacheService.getCacheStatistics()
        );
    }

    /**
     * 匹配MQTT主题模式
     */
    private boolean matchesTopicPattern(String topic, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }

        // 简单的通配符匹配
        String regex = pattern
                .replace("+", "[^/]+")     // + 匹配单个层级
                .replace("#", ".*")        // # 匹配多个层级
                .replace("/", "\\/");       // 转义斜杠

        return topic.matches(regex);
    }

    /**
     * 预热设备类型缓存
     */
    public void warmupCache() {
        try {
            // 预加载所有启用的设备类型
            deviceTypeCacheService.getEnabledDeviceTypes();

            log.info("设备类型路由器缓存预热完成");
        } catch (Exception e) {
            log.error("设备类型路由器缓存预热失败", e);
        }
    }

    /**
     * 路由统计信息
     */
    public static class RoutingStatistics {
        private final int hotCacheSize;
        private final int registeredProcessors;
        private final DeviceTypeCacheService.CacheStatistics cacheStatistics;

        public RoutingStatistics(int hotCacheSize, int registeredProcessors,
                                DeviceTypeCacheService.CacheStatistics cacheStatistics) {
            this.hotCacheSize = hotCacheSize;
            this.registeredProcessors = registeredProcessors;
            this.cacheStatistics = cacheStatistics;
        }

        public int getHotCacheSize() { return hotCacheSize; }
        public int getRegisteredProcessors() { return registeredProcessors; }
        public DeviceTypeCacheService.CacheStatistics getCacheStatistics() { return cacheStatistics; }

        @Override
        public String toString() {
            return String.format("RoutingStats{hotCache=%d, processors=%d, cache=%s}",
                               hotCacheSize, registeredProcessors, cacheStatistics);
        }
    }
}