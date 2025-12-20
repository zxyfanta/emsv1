package com.ems.service;

import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceGroup;
import com.ems.repository.device.DeviceRepository;
import com.ems.repository.device.DeviceGroupRepository;
import com.ems.repository.device.DeviceGroupMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 设备Redis缓存服务
 * 提供设备信息的Redis缓存功能，提升MQTT消息处理性能
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceGroupMappingRepository mappingRepository;

    // Redis Key前缀
    private static final String DEVICE_CACHE_PREFIX = "device_cache:";
    private static final String DEVICE_GROUP_PREFIX = "device_group:";

    // 缓存过期时间（秒）
    private static final long DEVICE_CACHE_EXPIRE_SECONDS = TimeUnit.HOURS.toSeconds(24);
    private static final long GROUP_CACHE_EXPIRE_SECONDS = TimeUnit.HOURS.toSeconds(12);

    /**
     * 设备缓存信息DTO
     */
    public static class DeviceInfo {
        private final Long id;
        private final String deviceId;
        private final String deviceName;
        private final Long enterpriseId;
        private final String status;
        private final Boolean deleted;

        public DeviceInfo(Long id, String deviceId, String deviceName, Long enterpriseId, String status, Boolean deleted) {
            this.id = id;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.enterpriseId = enterpriseId;
            this.status = status;
            this.deleted = deleted;
        }

        /**
         * 从Device实体创建DeviceInfo
         */
        public static DeviceInfo fromDevice(Device device) {
            return new DeviceInfo(
                device.getId(),
                device.getDeviceId(),
                device.getDeviceName(),
                device.getEnterpriseId(),
                device.getStatus().name(),
                device.getDeleted()
            );
        }

        // Getters
        public Long getId() { return id; }
        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public Long getEnterpriseId() { return enterpriseId; }
        public String getStatus() { return status; }
        public Boolean getDeleted() { return deleted; }

        /**
         * 检查设备是否活跃（未删除且存在）
         */
        public boolean isActive() {
            return Boolean.FALSE.equals(deleted);
        }
    }

    /**
     * 获取设备信息（优先从Redis缓存查询）
     *
     * @param deviceId 设备ID
     * @return 设备信息Optional
     */
    public Optional<DeviceInfo> getDeviceInfo(String deviceId) {
        try {
            String key = DEVICE_CACHE_PREFIX + deviceId;

            // 优先从Redis缓存查询
            Object cachedObj = redisTemplate.opsForValue().get(key);
            if (cachedObj instanceof DeviceInfo cachedInfo) {
                log.debug("✅ 从Redis缓存获取设备信息: {}", deviceId);
                // 更新缓存过期时间（LRU策略）
                redisTemplate.expire(key, DEVICE_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                return Optional.of(cachedInfo);
            }

            // Redis缓存未命中，从MySQL查询
            log.debug("📋 Redis缓存未命中，从MySQL查询设备: {}", deviceId);
            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                DeviceInfo deviceInfo = DeviceInfo.fromDevice(device);

                // 缓存到Redis
                cacheDeviceInfo(deviceId, deviceInfo);
                log.info("💾 设备信息已缓存到Redis: {} ({})", deviceId, device.getDeviceName());

                return Optional.of(deviceInfo);
            } else {
                log.debug("❌ 设备不存在: {}", deviceId);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("❌ 获取设备信息失败: deviceId={}", deviceId, e);
            // 异常情况下直接查询MySQL
            try {
                return deviceRepository.findByDeviceId(deviceId)
                    .map(DeviceInfo::fromDevice);
            } catch (Exception dbException) {
                log.error("❌ MySQL查询也失败: deviceId={}", deviceId, dbException);
                return Optional.empty();
            }
        }
    }

    /**
     * 缓存设备信息到Redis
     *
     * @param deviceId 设备ID
     * @param deviceInfo 设备信息
     */
    public void cacheDeviceInfo(String deviceId, DeviceInfo deviceInfo) {
        try {
            String key = DEVICE_CACHE_PREFIX + deviceId;
            redisTemplate.opsForValue().set(key, deviceInfo, DEVICE_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("✅ 设备信息已缓存: {}", deviceId);
        } catch (Exception e) {
            log.error("❌ 缓存设备信息失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 从Redis缓存中移除设备信息
     *
     * @param deviceId 设备ID
     */
    public void removeDeviceCache(String deviceId) {
        try {
            String key = DEVICE_CACHE_PREFIX + deviceId;
            redisTemplate.delete(key);
            log.debug("🗑️ 设备缓存已移除: {}", deviceId);
        } catch (Exception e) {
            log.error("❌ 移除设备缓存失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 预加载所有活跃设备到Redis缓存
     */
    public void preloadActiveDevices() {
        try {
            log.info("🚀 开始预加载活跃设备到Redis缓存...");

            // 获取所有活跃设备（分页查询全部数据）
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<Device> devicePage = deviceRepository.findAllActive(pageable);
            List<Device> activeDevices = devicePage.getContent();

            int totalDevices = activeDevices.size();
            int successDevices = 0;

            log.info("📊 找到 {} 个活跃设备需要预加载", totalDevices);

            for (Device device : activeDevices) {
                try {
                    DeviceInfo deviceInfo = DeviceInfo.fromDevice(device);
                    cacheDeviceInfo(device.getDeviceId(), deviceInfo);
                    successDevices++;
                } catch (Exception e) {
                    log.warn("⚠️ 预加载设备失败: deviceId={}", device.getDeviceId(), e);
                }
            }

            log.info("✅ 设备预加载完成: 总数={}, 成功={}, 失败={}",
                    totalDevices, successDevices, totalDevices - successDevices);

        } catch (Exception e) {
            log.error("❌ 预加载设备到Redis失败", e);
        }
    }

    /**
     * 检查设备是否存在（优先使用缓存）
     *
     * @param deviceId 设备ID
     * @return 是否存在且活跃
     */
    public boolean isDeviceActive(String deviceId) {
        return getDeviceInfo(deviceId)
                .map(DeviceInfo::isActive)
                .orElse(false);
    }

    /**
     * 更新设备缓存状态
     *
     * @param deviceId 设备ID
     */
    public void refreshDeviceCache(String deviceId) {
        try {
            // 先移除旧缓存
            removeDeviceCache(deviceId);

            // 重新查询并缓存
            getDeviceInfo(deviceId);

        } catch (Exception e) {
            log.error("❌ 刷新设备缓存失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存中的设备数量
     */
    public long getCachedDeviceCount() {
        try {
            return redisTemplate.keys(DEVICE_CACHE_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("❌ 获取缓存统计失败", e);
            return 0;
        }
    }

    /**
     * 清空所有设备缓存
     */
    public void clearAllDeviceCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(DEVICE_CACHE_PREFIX + "*"));
            log.info("🗑️ 所有设备缓存已清空");
        } catch (Exception e) {
            log.error("❌ 清空设备缓存失败", e);
        }
    }

    // ===== 设备分组缓存相关方法 =====

    /**
     * 获取分组中的设备列表（优先从缓存查询）
     *
     * @param groupId 分组ID
     * @return 设备ID列表
     */
    @SuppressWarnings("unchecked")
    public List<Long> getDevicesInGroup(Long groupId) {
        try {
            String key = DEVICE_GROUP_PREFIX + groupId;

            // 优先从Redis缓存查询
            Object cachedObj = redisTemplate.opsForValue().get(key);
            if (cachedObj instanceof List<?> cachedList) {
                log.debug("✅ 从Redis缓存获取分组设备: {}", groupId);
                // 更新缓存过期时间
                redisTemplate.expire(key, GROUP_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                return (List<Long>) cachedList;
            }

            // 缓存未命中，从数据库查询
            log.debug("📋 缓存未命中，从数据库查询分组设备: {}", groupId);
            List<Long> deviceIds = mappingRepository.findDeviceIdsByGroupId(groupId);

            // 缓存到Redis
            cacheGroupDevices(groupId, deviceIds);
            log.info("💾 分组设备已缓存: {} ({}个设备)", groupId, deviceIds.size());

            return deviceIds;

        } catch (Exception e) {
            log.error("❌ 获取分组设备失败: groupId={}", groupId, e);
            // 异常情况下直接查询数据库
            try {
                return mappingRepository.findDeviceIdsByGroupId(groupId);
            } catch (Exception dbException) {
                log.error("❌ 数据库查询也失败: groupId={}", groupId, dbException);
                return List.of();
            }
        }
    }

    /**
     * 缓存分组设备列表到Redis
     *
     * @param groupId 分组ID
     * @param deviceIds 设备ID列表
     */
    public void cacheGroupDevices(Long groupId, List<Long> deviceIds) {
        try {
            String key = DEVICE_GROUP_PREFIX + groupId;
            redisTemplate.opsForValue().set(key, deviceIds, GROUP_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("✅ 分组设备已缓存: {}", groupId);
        } catch (Exception e) {
            log.error("❌ 缓存分组设备失败: groupId={}", groupId, e);
        }
    }

    /**
     * 移除分组设备缓存
     *
     * @param groupId 分组ID
     */
    public void removeGroupDevicesCache(Long groupId) {
        try {
            String key = DEVICE_GROUP_PREFIX + groupId;
            redisTemplate.delete(key);
            log.debug("🗑️ 分组设备缓存已移除: {}", groupId);
        } catch (Exception e) {
            log.error("❌ 移除分组设备缓存失败: groupId={}", groupId, e);
        }
    }

    /**
     * 刷新分组设备缓存
     *
     * @param groupId 分组ID
     */
    public void refreshGroupDevicesCache(Long groupId) {
        try {
            // 先移除旧缓存
            removeGroupDevicesCache(groupId);

            // 重新查询并缓存
            getDevicesInGroup(groupId);

            log.debug("🔄 分组设备缓存已刷新: {}", groupId);
        } catch (Exception e) {
            log.error("❌ 刷新分组设备缓存失败: groupId={}", groupId, e);
        }
    }

    /**
     * 添加设备到分组缓存
     *
     * @param groupId 分组ID
     * @param deviceId 设备ID
     */
    public void addDeviceToGroupCache(Long groupId, Long deviceId) {
        try {
            // 获取当前缓存列表
            List<Long> deviceIds = getDevicesInGroup(groupId);

            // 添加新设备（避免重复）
            if (!deviceIds.contains(deviceId)) {
                deviceIds.add(deviceId);
                cacheGroupDevices(groupId, deviceIds);
                log.debug("✅ 设备已添加到分组缓存: groupId={}, deviceId={}", groupId, deviceId);
            }
        } catch (Exception e) {
            log.error("❌ 添加设备到分组缓存失败: groupId={}, deviceId={}", groupId, deviceId, e);
            // 失败时直接刷新整个缓存
            refreshGroupDevicesCache(groupId);
        }
    }

    /**
     * 从分组缓存中移除设备
     *
     * @param groupId 分组ID
     * @param deviceId 设备ID
     */
    public void removeDeviceFromGroupCache(Long groupId, Long deviceId) {
        try {
            // 获取当前缓存列表
            List<Long> deviceIds = getDevicesInGroup(groupId);

            // 移除设备
            if (deviceIds.remove(deviceId)) {
                cacheGroupDevices(groupId, deviceIds);
                log.debug("✅ 设备已从分组缓存移除: groupId={}, deviceId={}", groupId, deviceId);
            }
        } catch (Exception e) {
            log.error("❌ 从分组缓存移除设备失败: groupId={}, deviceId={}", groupId, deviceId, e);
            // 失败时直接刷新整个缓存
            refreshGroupDevicesCache(groupId);
        }
    }

    /**
     * 清空所有分组设备缓存
     */
    public void clearAllGroupCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(DEVICE_GROUP_PREFIX + "*"));
            log.info("🗑️ 所有分组设备缓存已清空");
        } catch (Exception e) {
            log.error("❌ 清空分组设备缓存失败", e);
        }
    }
}