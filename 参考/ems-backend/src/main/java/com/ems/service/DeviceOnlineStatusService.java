package com.ems.service;

import com.ems.entity.device.Device;
import com.ems.repository.device.DeviceRepository;
import com.ems.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备在线状态服务
 * 基于Redis实时数据判断设备在线状态
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceOnlineStatusService {

    private final RedisCacheService redisCacheService;
    private final AlertService alertService;
    private final DeviceRepository deviceRepository;

    // 配置参数
    private final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);  // 在线阈值
    private final Duration WARNING_THRESHOLD = Duration.ofMinutes(10); // 告警阈值

    /**
     * 获取设备在线状态
     */
    public DeviceOnlineStatus getDeviceOnlineStatus(String deviceId) {
        try {
            // 1. 检查实时CPM数据
            List<RedisCacheService.RealtimeDataPoint> cpmData =
                redisCacheService.getRealtimeCpmData(deviceId);

            // 2. 检查实时电池数据
            List<RedisCacheService.RealtimeDataPoint> batteryData =
                redisCacheService.getRealtimeBatteryData(deviceId);

            // 3. 获取最后数据时间
            LocalDateTime lastDataTime = getLastDataTime(cpmData, batteryData);

            // 4. 判断在线状态
            return determineOnlineStatus(deviceId, lastDataTime);

        } catch (Exception e) {
            log.error("获取设备在线状态失败: 设备={}", deviceId, e);
            return DeviceOnlineStatus.unknown(deviceId);
        }
    }

    /**
     * 批量检查设备在线状态
     */
    public Map<String, DeviceOnlineStatus> batchGetOnlineStatus(List<String> deviceIds) {
        Map<String, DeviceOnlineStatus> results = new HashMap<>();

        for (String deviceId : deviceIds) {
            results.put(deviceId, getDeviceOnlineStatus(deviceId));
        }

        return results;
    }

    /**
     * 智能获取设备在线状态（带回退机制）
     */
    public Device.DeviceStatus getSmartDeviceStatus(String deviceId) {
        try {
            // 优先使用Redis实时数据
            DeviceOnlineStatus onlineStatus = getDeviceOnlineStatus(deviceId);
            return onlineStatus.getDeviceStatus();
        } catch (Exception e) {
            log.warn("Redis状态获取失败，回退到数据库查询: 设备={}", deviceId, e);
            return deviceRepository.findByDeviceId(deviceId)
                .map(Device::getStatus)
                .orElse(Device.DeviceStatus.OFFLINE);
        }
    }

    /**
     * 定时检查设备离线告警
     */
    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void checkOfflineAlerts() {
        try {
            log.debug("开始定时检查设备离线告警");

            // 获取用户有权限的设备
            List<Device> devices = getUserAccessibleDevices();

            int checkedCount = 0;
            int alertTriggered = 0;

            for (Device device : devices) {
                try {
                    DeviceOnlineStatus status = getDeviceOnlineStatus(device.getDeviceId());

                    // 处理状态变化
                    if (handleStatusChange(device, status)) {
                        alertTriggered++;
                    }

                    checkedCount++;

                } catch (Exception e) {
                    log.warn("检查设备状态失败: 设备={}", device.getDeviceId(), e);
                }
            }

            log.info("定时设备状态检查完成: 检查{}台设备, 触发{}条告警", checkedCount, alertTriggered);

        } catch (Exception e) {
            log.error("定时检查设备离线告警失败", e);
        }
    }

    /**
     * 获取设备在线状态统计
     */
    public DeviceOnlineStats getDeviceOnlineStats() {
        try {
            List<Device> devices = getUserAccessibleDevices();

            Map<String, DeviceOnlineStatus> onlineStatuses = batchGetOnlineStatus(
                devices.stream().map(Device::getDeviceId).collect(Collectors.toList())
            );

            long onlineCount = onlineStatuses.values().stream()
                .filter(status -> status.getStatus() == DeviceOnlineStatus.Status.ONLINE)
                .count();

            long offlineCount = onlineStatuses.values().stream()
                .filter(status -> status.getStatus() == DeviceOnlineStatus.Status.OFFLINE)
                .count();

            long warningCount = onlineStatuses.values().stream()
                .filter(status -> status.getStatus() == DeviceOnlineStatus.Status.WARNING)
                .count();

            return new DeviceOnlineStats(devices.size(), onlineCount, offlineCount, warningCount);

        } catch (Exception e) {
            log.error("获取设备在线状态统计失败", e);
            return new DeviceOnlineStats(0, 0, 0, 0);
        }
    }

    /**
     * 获取最后数据时间
     */
    private LocalDateTime getLastDataTime(
            List<RedisCacheService.RealtimeDataPoint> cpmData,
            List<RedisCacheService.RealtimeDataPoint> batteryData) {

        LocalDateTime latest = LocalDateTime.MIN;

        for (RedisCacheService.RealtimeDataPoint point : cpmData) {
            if (point.getTimestamp().isAfter(latest)) {
                latest = point.getTimestamp();
            }
        }

        for (RedisCacheService.RealtimeDataPoint point : batteryData) {
            if (point.getTimestamp().isAfter(latest)) {
                latest = point.getTimestamp();
            }
        }

        return latest == LocalDateTime.MIN ? null : latest;
    }

    /**
     * 判断在线状态
     */
    private DeviceOnlineStatus determineOnlineStatus(String deviceId, LocalDateTime lastDataTime) {
        if (lastDataTime == null) {
            // 检查数据库中的最后在线时间
            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
            if (deviceOpt.isPresent() && deviceOpt.get().getLastOnlineAt() != null) {
                LocalDateTime lastOnline = deviceOpt.get().getLastOnlineAt();
                Duration offlineDuration = Duration.between(lastOnline, LocalDateTime.now());

                if (offlineDuration.compareTo(ONLINE_THRESHOLD) <= 0) {
                    return DeviceOnlineStatus.online(deviceId, lastOnline);
                } else {
                    return DeviceOnlineStatus.offline(deviceId, lastOnline, offlineDuration);
                }
            }

            return DeviceOnlineStatus.neverSeen(deviceId);
        }

        Duration offlineDuration = Duration.between(lastDataTime, LocalDateTime.now());

        if (offlineDuration.compareTo(ONLINE_THRESHOLD) <= 0) {
            return DeviceOnlineStatus.online(deviceId, lastDataTime);
        } else if (offlineDuration.compareTo(WARNING_THRESHOLD) <= 0) {
            return DeviceOnlineStatus.warning(deviceId, lastDataTime, offlineDuration);
        } else {
            return DeviceOnlineStatus.offline(deviceId, lastDataTime, offlineDuration);
        }
    }

    /**
     * 处理设备状态变化
     */
    private boolean handleStatusChange(Device device, DeviceOnlineStatus status) {
        // 只处理离线状态，避免重复告警
        if (status.getStatus() == DeviceOnlineStatus.Status.OFFLINE) {
            alertService.checkDeviceOfflineAlert(device, status.getLastDataTime());
            return true;
        }
        return false;
    }

    /**
     * 获取用户有权限的设备列表
     */
    private List<Device> getUserAccessibleDevices() {
        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            if (SecurityUtils.isPlatformAdmin()) {
                return deviceRepository.findAllActive(
                org.springframework.data.domain.PageRequest.of(0, 10000)
            ).getContent();
            } else if (userEnterpriseId != null) {
                return deviceRepository.findByEnterpriseId(userEnterpriseId);
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("获取用户权限设备列表失败，返回空列表", e);
            return new ArrayList<>();
        }
    }

    /**
     * 设备在线状态返回对象
     */
    public static class DeviceOnlineStatus {
        private final String deviceId;
        private final Status status;
        private final LocalDateTime lastDataTime;
        private final Duration offlineDuration;

        // 状态枚举
        public enum Status {
            ONLINE("在线", "#28a745"),
            WARNING("警告", "#ffc107"),
            OFFLINE("离线", "#dc3545"),
            NEVER_SEEN("从未上线", "#6c757d"),
            UNKNOWN("未知", "#6c757d");

            private final String description;
            private final String color;

            Status(String description, String color) {
                this.description = description;
                this.color = color;
            }

            public String getDescription() { return description; }
            public String getColor() { return color; }
        }

        // 静态工厂方法
        public static DeviceOnlineStatus online(String deviceId, LocalDateTime lastDataTime) {
            return new DeviceOnlineStatus(deviceId, Status.ONLINE, lastDataTime, Duration.ZERO);
        }

        public static DeviceOnlineStatus warning(String deviceId, LocalDateTime lastDataTime, Duration duration) {
            return new DeviceOnlineStatus(deviceId, Status.WARNING, lastDataTime, duration);
        }

        public static DeviceOnlineStatus offline(String deviceId, LocalDateTime lastDataTime, Duration duration) {
            return new DeviceOnlineStatus(deviceId, Status.OFFLINE, lastDataTime, duration);
        }

        public static DeviceOnlineStatus neverSeen(String deviceId) {
            return new DeviceOnlineStatus(deviceId, Status.NEVER_SEEN, null, null);
        }

        public static DeviceOnlineStatus unknown(String deviceId) {
            return new DeviceOnlineStatus(deviceId, Status.UNKNOWN, null, null);
        }

        // 转换为设备状态
        public Device.DeviceStatus getDeviceStatus() {
            return switch (status) {
                case ONLINE, WARNING -> Device.DeviceStatus.ONLINE;
                case OFFLINE -> Device.DeviceStatus.OFFLINE;
                default -> Device.DeviceStatus.UNKNOWN;
            };
        }

        // 构造函数
        public DeviceOnlineStatus(String deviceId, Status status, LocalDateTime lastDataTime, Duration offlineDuration) {
            this.deviceId = deviceId;
            this.status = status;
            this.lastDataTime = lastDataTime;
            this.offlineDuration = offlineDuration;
        }

        // Getters
        public String getDeviceId() { return deviceId; }
        public Status getStatus() { return status; }
        public LocalDateTime getLastDataTime() { return lastDataTime; }
        public Duration getOfflineDuration() { return offlineDuration; }

        public String getOfflineDurationText() {
            if (offlineDuration == null || offlineDuration.isZero()) {
                return "";
            }

            long hours = offlineDuration.toHours();
            long minutes = offlineDuration.toMinutesPart();

            if (hours > 0) {
                return String.format("%d小时%d分钟", hours, minutes);
            } else {
                return String.format("%d分钟", minutes);
            }
        }
    }

    /**
     * 设备在线状态统计
     */
    public static class DeviceOnlineStats {
        private final long totalCount;
        private final long onlineCount;
        private final long offlineCount;
        private final long warningCount;

        public DeviceOnlineStats(long totalCount, long onlineCount, long offlineCount, long warningCount) {
            this.totalCount = totalCount;
            this.onlineCount = onlineCount;
            this.offlineCount = offlineCount;
            this.warningCount = warningCount;
        }

        public long getTotalCount() { return totalCount; }
        public long getOnlineCount() { return onlineCount; }
        public long getOfflineCount() { return offlineCount; }
        public long getWarningCount() { return warningCount; }

        public double getOnlineRate() {
            return totalCount > 0 ? (double) onlineCount / totalCount * 100 : 0;
        }

        public double getOfflineRate() {
            return totalCount > 0 ? (double) offlineCount / totalCount * 100 : 0;
        }
    }
}