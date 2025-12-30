package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备信息缓存服务
 *
 * 功能:
 * 1. 缓存设备基础信息(Device实体),减少数据库查询
 * 2. 提供缓存查询、更新、失效方法
 * 3. 支持缓存预热
 * 4. TTL随机化避免缓存雪崩
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceRepository deviceRepository;

    private static final String CACHE_PREFIX = "device:info:";
    private static final long CACHE_TTL_MINUTES = 5;  // 缩短至5分钟,提升数据一致性

    /**
     * 根据设备编码获取设备信息(带缓存)
     *
     * 缓存策略: Cache Aside
     * 1. 先从Redis获取
     * 2. 未命中则从数据库加载并写入Redis
     * 3. TTL为5分钟±1分钟(避免缓存雪崩)
     *
     * @param deviceCode 设备编码
     * @return 设备信息,不存在则返回null
     */
    public Device getDevice(String deviceCode) {
        String key = CACHE_PREFIX + deviceCode;

        try {
            // 1. 从Redis获取
            Device cached = (Device) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("设备缓存命中: {}", deviceCode);
                return cached;
            }

            // 2. 从数据库加载
            log.debug("设备缓存未命中,从数据库加载: {}", deviceCode);
            Device device = deviceRepository.findByDeviceCode(deviceCode).orElse(null);

            if (device != null) {
                // TTL随机化避免缓存雪崩(5±1分钟)
                long ttl = CACHE_TTL_MINUTES + (long)(Math.random() * 2);
                redisTemplate.opsForValue().set(key, device, ttl, TimeUnit.MINUTES);
                log.debug("设备信息已缓存: {}, TTL: {}分钟", deviceCode, ttl);
            } else {
                log.debug("设备不存在: {}", deviceCode);
            }

            return device;
        } catch (Exception e) {
            log.error("获取设备缓存异常: {}, 降级到数据库查询", deviceCode, e);
            // 降级: 直接查询数据库
            return deviceRepository.findByDeviceCode(deviceCode).orElse(null);
        }
    }

    /**
     * 清除设备缓存
     *
     * 使用场景: 设备信息更新、删除时调用
     *
     * @param deviceCode 设备编码
     */
    public void evictDevice(String deviceCode) {
        String key = CACHE_PREFIX + deviceCode;

        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("清除设备缓存: {}, 结果: {}", deviceCode, deleted);
        } catch (Exception e) {
            log.error("清除设备缓存失败: {}", deviceCode, e);
        }
    }

    /**
     * 预热所有设备缓存
     *
     * 使用场景: 应用启动时调用,将所有设备信息加载到Redis
     *
     * 注意: 只缓存启用了数据上报的设备,减少内存占用
     */
    public void warmUpCache() {
        log.info("开始预热设备缓存...");

        try {
            // 只查询启用了数据上报的辐射设备和环境设备
            List<Device> radiationDevices = deviceRepository
                .findByDeviceTypeAndDataReportEnabledTrue(com.cdutetc.ems.entity.enums.DeviceType.RADIATION_MONITOR);
            List<Device> environmentDevices = deviceRepository
                .findByDeviceTypeAndDataReportEnabledTrue(com.cdutetc.ems.entity.enums.DeviceType.ENVIRONMENT_STATION);

            List<Device> devices = new java.util.ArrayList<>();
            devices.addAll(radiationDevices);
            devices.addAll(environmentDevices);

            int cachedCount = 0;
            for (Device device : devices) {
                String key = CACHE_PREFIX + device.getDeviceCode();
                try {
                    redisTemplate.opsForValue().set(key, device, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                    cachedCount++;
                } catch (Exception e) {
                    log.warn("缓存设备失败: {}", device.getDeviceCode(), e);
                }
            }

            log.info("设备缓存预热完成, 成功缓存 {} 个设备", cachedCount);
        } catch (Exception e) {
            log.error("设备缓存预热失败", e);
        }
    }
}
