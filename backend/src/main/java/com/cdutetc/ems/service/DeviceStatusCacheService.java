package com.cdutetc.ems.service;

import com.cdutetc.ems.dto.cache.DeviceStatusCache;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备状态缓存服务
 *
 * 功能：
 * 1. 缓存设备实时状态（高频更新）
 * 2. 定时同步到数据库（低频持久化）
 * 3. 启动时预热（从数据库加载）
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceRepository deviceRepository;

    private static final String CACHE_KEY_PREFIX = "device:status:";
    private static final long CACHE_TTL_SECONDS = 600;  // 10分钟
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 更新设备最后消息时间
     * 每次收到MQTT消息时调用
     */
    public void updateLastMessageTime(String deviceCode, LocalDateTime messageTime) {
        String key = buildCacheKey(deviceCode);

        // 使用Hash存储，支持部分字段更新
        redisTemplate.opsForHash().put(key, "lastMessageAt",
            messageTime.format(ISO_FORMATTER));

        // 刷新TTL
        redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("更新设备最后消息时间: {} -> {}", deviceCode, messageTime);
    }

    /**
     * 更新设备CPM值
     * 每次收到辐射数据时调用
     */
    public void updateLastCpm(String deviceCode, Double cpm) {
        String key = buildCacheKey(deviceCode);

        redisTemplate.opsForHash().put(key, "lastCpm", String.valueOf(cpm));
        redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("更新设备CPM值: {} -> {}", deviceCode, cpm);
    }

    /**
     * 更新设备电压值
     * 每次收到环境数据时调用
     */
    public void updateLastBattery(String deviceCode, Double battery) {
        String key = buildCacheKey(deviceCode);

        redisTemplate.opsForHash().put(key, "lastBattery", String.valueOf(battery));
        redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("更新设备电压值: {} -> {}", deviceCode, battery);
    }

    /**
     * 更新设备状态
     */
    public void updateStatus(String deviceCode, String status) {
        String key = buildCacheKey(deviceCode);

        redisTemplate.opsForHash().put(key, "status", status);
        redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("更新设备状态: {} -> {}", deviceCode, status);
    }

    /**
     * 获取设备最后消息时间
     */
    public LocalDateTime getLastMessageTime(String deviceCode) {
        String key = buildCacheKey(deviceCode);
        Object value = redisTemplate.opsForHash().get(key, "lastMessageAt");

        if (value != null) {
            return LocalDateTime.parse(value.toString(), ISO_FORMATTER);
        }
        return null;
    }

    /**
     * 获取设备最后CPM值
     */
    public Double getLastCpm(String deviceCode) {
        String key = buildCacheKey(deviceCode);
        Object value = redisTemplate.opsForHash().get(key, "lastCpm");

        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        return null;
    }

    /**
     * 获取设备最后电压值
     */
    public Double getLastBattery(String deviceCode) {
        String key = buildCacheKey(deviceCode);
        Object value = redisTemplate.opsForHash().get(key, "lastBattery");

        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        return null;
    }

    /**
     * 获取设备完整状态缓存
     */
    public DeviceStatusCache getDeviceStatus(String deviceCode) {
        String key = buildCacheKey(deviceCode);

        Object deviceId = redisTemplate.opsForHash().get(key, "deviceId");
        if (deviceId == null) {
            // 缓存未命中，返回null
            return null;
        }

        DeviceStatusCache status = new DeviceStatusCache();
        status.setDeviceCode(deviceCode);
        status.setDeviceId(Long.parseLong(deviceId.toString()));
        status.setLastMessageAt(getStringValue(key, "lastMessageAt"));
        status.setLastCpm(getDoubleValue(key, "lastCpm"));
        status.setLastBattery(getDoubleValue(key, "lastBattery"));
        status.setStatus(getStringValue(key, "status"));
        status.setCompanyId(getLongValue(key, "companyId"));

        return status;
    }

    /**
     * 设置设备状态缓存（完整对象）
     * 用于启动时预热
     */
    public void setDeviceStatus(DeviceStatusCache status) {
        String key = buildCacheKey(status.getDeviceCode());

        if (status.getLastMessageAt() != null) {
            redisTemplate.opsForHash().put(key, "lastMessageAt", status.getLastMessageAt());
        }
        if (status.getLastCpm() != null) {
            redisTemplate.opsForHash().put(key, "lastCpm", String.valueOf(status.getLastCpm()));
        }
        if (status.getLastBattery() != null) {
            redisTemplate.opsForHash().put(key, "lastBattery", String.valueOf(status.getLastBattery()));
        }
        if (status.getStatus() != null) {
            redisTemplate.opsForHash().put(key, "status", status.getStatus());
        }
        if (status.getCompanyId() != null) {
            redisTemplate.opsForHash().put(key, "companyId", String.valueOf(status.getCompanyId()));
        }
        if (status.getDeviceId() != null) {
            redisTemplate.opsForHash().put(key, "deviceId", String.valueOf(status.getDeviceId()));
        }

        redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("设置设备状态缓存: {}", status.getDeviceCode());
    }

    /**
     * 删除设备状态缓存
     * 用于设备删除或状态重置
     */
    public void deleteDeviceStatus(String deviceCode) {
        String key = buildCacheKey(deviceCode);
        redisTemplate.delete(key);
        log.debug("删除设备状态缓存: {}", deviceCode);
    }

    /**
     * 批量预热设备状态
     * 应用启动时调用，从数据库加载所有设备到Redis
     */
    public void warmUpCache() {
        log.info("开始预热设备状态缓存...");

        try {
            // 获取所有已激活的设备
            List<Device> devices = deviceRepository.findAll();

            int count = 0;
            for (Device device : devices) {
                DeviceStatusCache status = new DeviceStatusCache();
                status.setDeviceCode(device.getDeviceCode());
                status.setDeviceId(device.getId());
                status.setCompanyId(device.getCompany() != null ? device.getCompany().getId() : null);
                status.setStatus(device.getStatus().name());

                // 使用lastOnlineAt作为初始lastMessageAt
                if (device.getLastOnlineAt() != null) {
                    status.setLastMessageAt(device.getLastOnlineAt().format(ISO_FORMATTER));
                }

                setDeviceStatus(status);
                count++;
            }

            log.info("设备状态缓存预热完成，共加载 {} 个设备", count);
        } catch (Exception e) {
            log.error("预热设备状态缓存失败", e);
        }
    }

    /**
     * 刷新缓存到数据库
     * 定时任务调用，将Redis中的状态同步到数据库
     */
    public void flushToDatabase() {
        log.info("开始同步设备状态到数据库...");

        try {
            // 扫描所有设备状态缓存键
            String pattern = CACHE_KEY_PREFIX + "*";

            // 注意：这里使用keys命令在生产环境要注意性能
            // 可以考虑使用Scan命令替代
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                int updateCount = 0;

                for (String key : keys) {
                    DeviceStatusCache status = getDeviceStatus(
                        key.substring(CACHE_KEY_PREFIX.length())
                    );

                    if (status != null && status.getDeviceId() != null) {
                        // 更新数据库
                        Device device = deviceRepository.findById(status.getDeviceId())
                            .orElse(null);

                        if (device != null) {
                            // 只更新必要的字段
                            if (status.getLastMessageAt() != null) {
                                device.setLastOnlineAt(
                                    LocalDateTime.parse(status.getLastMessageAt(), ISO_FORMATTER)
                                );
                            }

                            deviceRepository.save(device);
                            updateCount++;
                        }
                    }
                }

                log.info("设备状态同步完成，共更新 {} 个设备", updateCount);
            }
        } catch (Exception e) {
            log.error("同步设备状态到数据库失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    private String buildCacheKey(String deviceCode) {
        return CACHE_KEY_PREFIX + deviceCode;
    }

    private String getStringValue(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    private Double getDoubleValue(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        return null;
    }

    private Long getLongValue(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        return null;
    }
}
