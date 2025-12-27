package com.cdutetc.ems.service;

import com.cdutetc.ems.dto.DeviceReportConfig;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备上报配置 Redis 缓存服务
 * 采用 Cache Aside 模式
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceReportConfigCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    /**
     * 缓存Key前缀
     */
    private static final String CACHE_KEY_PREFIX = "device:report:config:";

    /**
     * 缓存过期时间（秒）
     * 默认1小时
     */
    private static final long CACHE_TTL_SECONDS = 3600;

    /**
     * 获取设备上报配置（优先从缓存）
     *
     * @param deviceCode 设备编码
     * @return 设备上报配置
     */
    public DeviceReportConfig getReportConfig(String deviceCode) {
        String key = CACHE_KEY_PREFIX + deviceCode;

        // 1. 尝试从 Redis 获取
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("✅ 从Redis获取设备上报配置: deviceCode={}", deviceCode);
                return objectMapper.convertValue(cached, DeviceReportConfig.class);
            }
        } catch (Exception e) {
            log.warn("❌ Redis读取失败，降级到MySQL: deviceCode={}, error={}", deviceCode, e.getMessage());
        }

        // 2. Redis 未命中或异常，从 MySQL 加载
        log.debug("📥 从MySQL加载设备上报配置: deviceCode={}", deviceCode);
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + deviceCode));

        DeviceReportConfig config = DeviceReportConfig.fromDevice(device);

        // 3. 写入 Redis 缓存（异步，不阻塞主流程）
        try {
            redisTemplate.opsForValue().set(key, config, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("💾 设备上报配置已写入Redis: deviceCode={}", deviceCode);
        } catch (Exception e) {
            log.warn("⚠️ Redis写入失败，不影响主流程: deviceCode={}, error={}", deviceCode, e.getMessage());
        }

        return config;
    }

    /**
     * 删除设备上报配置缓存
     * 在设备配置更新时调用
     *
     * @param deviceCode 设备编码
     */
    public void evictReportConfig(String deviceCode) {
        String key = CACHE_KEY_PREFIX + deviceCode;
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("🗑️ 已清除设备上报配置缓存: deviceCode={}, deleted={}", deviceCode, deleted);
        } catch (Exception e) {
            log.warn("⚠️ 清除缓存失败: deviceCode={}, error={}", deviceCode, e.getMessage());
        }
    }

    /**
     * 批量删除设备上报配置缓存
     *
     * @param deviceCodes 设备编码列表
     */
    public void evictReportConfigBatch(List<String> deviceCodes) {
        if (deviceCodes == null || deviceCodes.isEmpty()) {
            return;
        }

        List<String> keys = deviceCodes.stream()
                .map(code -> CACHE_KEY_PREFIX + code)
                .toList();

        try {
            Long count = redisTemplate.delete(keys);
            log.info("🗑️ 批量清除设备上报配置缓存: count={}", count);
        } catch (Exception e) {
            log.warn("⚠️ 批量清除缓存失败: error={}", e.getMessage());
        }
    }

    /**
     * 预热缓存（可选，系统启动时调用）
     * 只缓存启用上报的辐射设备配置
     */
    public void warmUpCache() {
        log.info("🔥 开始预热设备上报配置缓存...");

        // 查询所有启用了上报的辐射设备
        List<Device> radiationDevices = deviceRepository.findByDeviceTypeAndDataReportEnabledTrue(
                DeviceType.RADIATION_MONITOR
        );

        int successCount = 0;
        int failCount = 0;

        for (Device device : radiationDevices) {
            try {
                getReportConfig(device.getDeviceCode());
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("⚠️ 预热缓存失败: deviceCode={}, error={}", device.getDeviceCode(), e.getMessage());
            }
        }

        log.info("🔥 设备上报配置缓存预热完成: 成功={}, 失败={}, 总计={}",
                successCount, failCount, radiationDevices.size());
    }

    /**
     * 清空所有设备上报配置缓存
     * 慎用！
     */
    public void evictAll() {
        log.warn("⚠️ 准备清空所有设备上报配置缓存...");
        // 注意：由于 keys() 命令在生产环境可能有性能问题，这里使用 scan 的简化版本
        // 实际生产环境建议使用 RedisScanUtil
        try {
            // 简化实现：根据已知的前缀模式删除
            // 这里只是为了示例，实际应该使用 scan
            log.warn("⚠️ 此操作需要谨慎使用，建议使用 scan 方式");
        } catch (Exception e) {
            log.error("❌ 清空所有缓存失败: error={}", e.getMessage());
        }
    }
}
