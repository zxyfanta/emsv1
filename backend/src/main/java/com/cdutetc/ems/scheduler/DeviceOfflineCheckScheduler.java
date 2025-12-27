package com.cdutetc.ems.scheduler;

import com.cdutetc.ems.entity.Alert;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.AlertSeverity;
import com.cdutetc.ems.entity.enums.AlertType;
import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
import com.cdutetc.ems.repository.AlertRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.service.AlertConfigService;
import com.cdutetc.ems.service.AlertService;
import com.cdutetc.ems.service.DeviceStatusCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 设备离线检查定时任务
 *
 * 功能：
 * 1. 定时扫描所有激活设备
 * 2. 检查最后消息时间
 * 3. 超时未收到消息 → 触发OFFLINE告警
 * 4. 设备重新上线 → 自动解决旧告警
 *
 * @author EMS Team
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DeviceOfflineCheckScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusCacheService cacheService;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final AlertConfigService alertConfigService;

    /**
     * 每分钟检查一次设备离线状态
     * initialDelay: 启动后1分钟开始第一次执行
     * fixedRate: 每1分钟执行一次（60000毫秒）
     */
    @Scheduled(initialDelay = 60000, fixedRate = 60000)
    public void checkDeviceOffline() {
        log.debug("开始检查设备离线状态...");

        try {
            // 1. 获取离线超时配置
            int timeoutMinutes = alertConfigService
                    .getOfflineTimeoutConfig().getTimeoutMinutes();

            LocalDateTime offlineThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

            // 2. 获取所有激活的设备
            List<Device> activeDevices = deviceRepository.findByActivationStatus(
                    DeviceActivationStatus.ACTIVE
            );

            int offlineCount = 0;
            int backOnlineCount = 0;

            // 3. 遍历检查每个设备
            for (Device device : activeDevices) {
                try {
                    // 从缓存获取最后消息时间
                    LocalDateTime lastMessageTime = cacheService.getLastMessageTime(
                            device.getDeviceCode()
                    );

                    // 缓存未命中，从数据库查询
                    if (lastMessageTime == null) {
                        lastMessageTime = device.getLastOnlineAt();
                    }

                    // 设备从未上线，跳过
                    if (lastMessageTime == null) {
                        continue;
                    }

                    // 判断是否离线
                    if (lastMessageTime.isBefore(offlineThreshold)) {
                        // 设备离线，检查是否已有告警
                        List<Alert> existingAlerts = alertRepository
                                .findByDeviceIdAndAlertTypeAndResolved(
                                        device.getId(),
                                        AlertType.OFFLINE.getCode(),
                                        false
                                );

                        if (existingAlerts.isEmpty()) {
                            // 触发离线告警
                            long offlineMinutes = ChronoUnit.MINUTES.between(
                                    lastMessageTime, LocalDateTime.now()
                            );

                            String offlineDuration = formatDuration(
                                    Duration.between(lastMessageTime, LocalDateTime.now())
                            );

                            alertService.createAlert(
                                    AlertType.OFFLINE,
                                    AlertSeverity.WARNING,
                                    device.getDeviceCode(),
                                    device.getId(),
                                    device.getCompany().getId(),
                                    MessageFormat.format(
                                            "设备离线: 最后消息时间为{0}，已离线{1}",
                                            lastMessageTime.format(
                                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                            ),
                                            offlineDuration
                                    ),
                                    java.util.Map.of(
                                            "lastMessageAt", lastMessageTime.toString(),
                                            "offlineMinutes", offlineMinutes
                                    )
                            );

                            // 更新设备状态为OFFLINE
                            cacheService.updateStatus(device.getDeviceCode(), "OFFLINE");

                            offlineCount++;
                        }
                    } else {
                        // 设备在线，解决旧告警
                        List<Alert> unresolvedAlerts = alertRepository
                                .findByDeviceIdAndAlertTypeAndResolved(
                                        device.getId(),
                                        AlertType.OFFLINE.getCode(),
                                        false
                                );

                        if (!unresolvedAlerts.isEmpty()) {
                            unresolvedAlerts.forEach(alert -> {
                                alert.setResolved(true);
                                alert.setResolvedAt(LocalDateTime.now());
                                alertRepository.save(alert);
                            });

                            backOnlineCount += unresolvedAlerts.size();
                        }
                    }

                } catch (Exception e) {
                    log.error("检查设备{}离线状态失败", device.getDeviceCode(), e);
                }
            }

            if (offlineCount > 0) {
                log.warn("⚠️ 发现{}个设备离线", offlineCount);
            }

            if (backOnlineCount > 0) {
                log.info("✅ {}个设备重新上线", backOnlineCount);
            }

        } catch (Exception e) {
            log.error("设备离线检查任务执行失败", e);
        }
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
}
