package com.cdutetc.ems.service;

import com.cdutetc.ems.config.AlertProperties;
import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.cdutetc.ems.entity.Alert;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.AlertSeverity;
import com.cdutetc.ems.entity.enums.AlertType;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.repository.AlertRepository;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 告警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;
    private final SseEmitterService sseEmitterService;
    private final AlertConfigService alertConfigService;
    private final DeviceStatusCacheService deviceStatusCacheService;
    private final AlertCacheService alertCacheService;
    private final ObjectMapper objectMapper;

    /**
     * 创建告警
     */
    @Transactional
    public Alert createAlert(AlertType alertType, AlertSeverity severity, String deviceCode,
                            Long deviceId, Long companyId, String message, Map<String, Object> data) {
        Alert alert = new Alert();
        alert.setAlertType(alertType.getCode());
        alert.setSeverity(severity.getCode());
        alert.setDeviceCode(deviceCode);
        alert.setMessage(message);
        alert.setResolved(false);

        // 设置设备
        if (deviceId != null) {
            Device device = deviceRepository.findById(deviceId).orElse(null);
            alert.setDevice(device);
        }

        // 设置企业
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("企业不存在"));
        alert.setCompany(company);

        // 序列化数据为JSON
        if (data != null) {
            try {
                alert.setData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                log.error("序列化告警数据失败", e);
            }
        }

        Alert saved = alertRepository.save(alert);
        log.info("创建告警成功: type={}, deviceCode={}, severity={}",
                alertType.getCode(), deviceCode, severity.getCode());

        // 通过SSE推送告警
        pushAlertViaSSE(saved);

        return saved;
    }

    /**
     * 通过SSE推送告警
     */
    private void pushAlertViaSSE(Alert alert) {
        try {
            Map<String, Object> alertData = Map.of(
                "alertId", alert.getId(),
                "alertType", alert.getAlertType(),
                "severity", alert.getSeverity(),
                "deviceCode", alert.getDeviceCode() != null ? alert.getDeviceCode() : "",
                "message", alert.getMessage(),
                "timestamp", alert.getCreatedAt().toString()
            );

            DeviceDataEvent event = new DeviceDataEvent(
                "alert",
                alert.getDeviceCode(),
                "ALERT",
                alertData
            );

            sseEmitterService.broadcastDeviceData(alert.getCompany().getId(), event);
            log.debug("🚨 SSE推送告警成功: {}", alert.getMessage());
        } catch (Exception e) {
            log.error("SSE推送告警失败", e);
        }
    }

    /**
     * 检查辐射数据并触发告警（CPM上升率检查）
     *
     * @param deviceCode 设备编码
     * @param cpm 当前CPM值
     * @param deviceType 设备类型（RADIATION 或 ENVIRONMENT）
     * @param deviceId 设备ID
     * @param companyId 企业ID
     */
    public void checkRadiationDataAndAlert(String deviceCode, Double cpm, String deviceType,
                                            Long deviceId, Long companyId) {
        if (cpm == null) {
            return;
        }

        // 1. 获取CPM上升率配置，根据设备类型选择合适的阈值
        AlertProperties.CpmRise config = alertConfigService.getCpmRiseConfig();
        double risePercentageThreshold = config.getRisePercentageForDevice(deviceType);

        // 2. 从缓存获取上次CPM值
        Double lastCpm = deviceStatusCacheService.getLastCpm(deviceCode);

        // 3. 首次启动或无历史数据，跳过
        if (lastCpm == null) {
            log.debug("设备{}首次记录CPM值: {}", deviceCode, cpm);
            return;
        }

        // 4. 检查最小CPM基数（避免基数太小导致误报）
        if (lastCpm < config.getMinCpm()) {
            log.debug("设备{}上次CPM值{}低于最小基数{}，跳过检查",
                      deviceCode, lastCpm, config.getMinCpm());
            return;
        }

        // 5. 计算上升率
        double riseRate = (cpm - lastCpm) / lastCpm;

        // 6. 检查上升率是否超过阈值
        if (riseRate <= risePercentageThreshold) {
            log.debug("设备{}({}) CPM上升率{}%未超过阈值{}%",
                      deviceCode, deviceType, riseRate * 100, risePercentageThreshold * 100);
            return;
        }

        // 7. 检查告警去重（最小间隔，防止频繁告警）
        LocalDateTime lastAlertTime = deviceStatusCacheService.getLastCpmRiseAlertTime(deviceCode);
        if (lastAlertTime != null) {
            long secondsSinceLastAlert = ChronoUnit.SECONDS.between(
                lastAlertTime, LocalDateTime.now()
            );
            if (secondsSinceLastAlert < config.getMinInterval()) {
                log.debug("设备{}距离上次告警仅{}秒，未超过最小间隔{}秒",
                          deviceCode, secondsSinceLastAlert, config.getMinInterval());
                return;
            }
        }

        // 8. 触发CPM上升率告警
        String message = String.format(
            "辐射值突增: 从%.2f CPM上升至%.2f CPM（上升%.1f%%），超过阈值%.0f%%",
            lastCpm, cpm, riseRate * 100, risePercentageThreshold * 100
        );

        createAlert(
            AlertType.CPM_RISE,
            AlertSeverity.CRITICAL,
            deviceCode,
            deviceId,
            companyId,
            message,
            Map.of(
                "lastCpm", lastCpm,
                "currentCpm", cpm,
                "riseRate", riseRate,
                "threshold", risePercentageThreshold,
                "deviceType", deviceType
            )
        );

        // 9. 更新告警去重缓存
        deviceStatusCacheService.updateLastCpmRiseAlertTime(deviceCode, LocalDateTime.now());

        log.warn("⚠️ CPM上升率告警触发: deviceCode={}, deviceType={}, riseRate={}%, lastCpm={}, currentCpm={}",
                 deviceCode, deviceType, String.format("%.1f", riseRate * 100),
                 String.format("%.2f", lastCpm), String.format("%.2f", cpm));
    }

    /**
     * 检查辐射数据并触发告警（CPM上升率检查）- 兼容旧方法
     * @deprecated 使用 checkRadiationDataAndAlert(deviceCode, cpm, deviceType, deviceId, companyId) 代替
     */
    @Deprecated
    public void checkRadiationDataAndAlert(String deviceCode, Double cpm, Long deviceId, Long companyId) {
        // 默认使用辐射设备类型
        checkRadiationDataAndAlert(deviceCode, cpm, "RADIATION", deviceId, companyId);
    }

    /**
     * 检查环境数据并触发告警（低电压检查）
     *
     * @param deviceCode 设备编码
     * @param battery 当前电压值（伏V）
     * @param deviceType 设备类型（RADIATION 或 ENVIRONMENT）
     * @param deviceId 设备ID
     * @param companyId 企业ID
     */
    public void checkEnvironmentDataAndAlert(String deviceCode, Double battery, String deviceType,
                                              Long deviceId, Long companyId) {
        if (battery == null) {
            return;
        }

        // 从配置服务读取低电压阈值，根据设备类型选择合适的阈值
        AlertProperties.LowBattery config = alertConfigService.getLowBatteryConfig();
        double voltageThreshold = config.getThresholdForDevice(deviceType);

        // 检查低电量
        if (battery < voltageThreshold) {
            createAlert(
                AlertType.LOW_BATTERY,
                AlertSeverity.WARNING,
                deviceCode,
                deviceId,
                companyId,
                String.format("电量不足: 当前电压%.2f V，低于阈值%.1f V", battery, voltageThreshold),
                Map.of("battery", battery, "threshold", voltageThreshold, "deviceType", deviceType)
            );

            log.warn("⚠️ 低电压告警触发: deviceCode={}, deviceType={}, battery={}V, threshold={}V",
                     deviceCode, deviceType, String.format("%.2f", battery), voltageThreshold);
        }
    }

    /**
     * 检查环境数据并触发告警（低电压检查）- 兼容旧方法
     * @deprecated 使用 checkEnvironmentDataAndAlert(deviceCode, battery, deviceType, deviceId, companyId) 代替
     */
    @Deprecated
    public void checkEnvironmentDataAndAlert(String deviceCode, Double battery, Long deviceId, Long companyId) {
        // 默认使用辐射设备阈值
        checkEnvironmentDataAndAlert(deviceCode, battery, "RADIATION", deviceId, companyId);
    }

    /**
     * 检查设备状态并触发告警
     */
    public void checkDeviceStatusAndAlert(Device device) {
        // 设备不再发送FAULT状态，已移除此检查
        // 设备故障告警逻辑已被移除，因为设备本身不传递故障信息

        // if (DeviceStatus.FAULT.name().equals(device.getStatus())) {
        //     createAlert(
        //         AlertType.FAULT,
        //         AlertSeverity.CRITICAL,
        //         device.getDeviceCode(),
        //         device.getId(),
        //         device.getCompany().getId(),
        //         "设备故障: 设备状态异常",
        //         Map.of("deviceStatus", device.getStatus())
        //     );
        // }

        // 检查设备离线（离线超过10分钟触发告警）
        if (DeviceStatus.OFFLINE.name().equals(device.getStatus())) {
            if (device.getLastOnlineAt() != null) {
                LocalDateTime offlineThreshold = LocalDateTime.now().minusMinutes(10);
                if (device.getLastOnlineAt().isBefore(offlineThreshold)) {
                    // 检查是否已有未解决的离线告警
                    List<Alert> existingAlerts = alertRepository.findByDeviceId(device.getId());
                    boolean hasOfflineAlert = existingAlerts.stream()
                            .anyMatch(a -> a.getAlertType().equals(AlertType.OFFLINE.getCode())
                                    && !a.getResolved());

                    if (!hasOfflineAlert) {
                        createAlert(
                            AlertType.OFFLINE,
                            AlertSeverity.WARNING,
                            device.getDeviceCode(),
                            device.getId(),
                            device.getCompany().getId(),
                            String.format("设备离线: 最后在线时间 %s", device.getLastOnlineAt()),
                            Map.of("lastOnlineAt", device.getLastOnlineAt().toString())
                        );
                    }
                }
            }
        }
    }

    /**
     * 解决告警
     */
    @Transactional
    public Alert resolveAlert(Long alertId, Long companyId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在"));

        if (!alert.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限操作此告警");
        }

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());

        Alert resolved = alertRepository.save(alert);
        log.info("告警已解决: id={}", alertId);
        return resolved;
    }

    /**
     * 批量解决告警
     * 优化: 使用批量保存saveAll()代替循环保存,提升性能
     * 缓存: 解决告警后清除设备告警缓存
     */
    @Transactional
    public int resolveAlertsByDevice(Long deviceId, Long companyId) {
        List<Alert> alerts = alertRepository.findByDeviceId(deviceId);
        List<Alert> alertsToUpdate = new java.util.ArrayList<>();

        for (Alert alert : alerts) {
            if (!alert.getResolved() && alert.getCompany().getId().equals(companyId)) {
                alert.setResolved(true);
                alert.setResolvedAt(LocalDateTime.now());
                alertsToUpdate.add(alert);
            }
        }

        // 批量保存,提升性能
        if (!alertsToUpdate.isEmpty()) {
            alertRepository.saveAll(alertsToUpdate);
            // 清除设备告警缓存
            alertCacheService.evictDeviceAlerts(deviceId);
        }

        log.info("批量解决告警: deviceId={}, count={}", deviceId, alertsToUpdate.size());
        return alertsToUpdate.size();
    }

    /**
     * 获取企业的告警列表（分页）
     */
    public Page<Alert> getAlerts(Long companyId, Pageable pageable) {
        return alertRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * 获取未解决的告警
     */
    public List<Alert> getUnresolvedAlerts(Long companyId) {
        return alertRepository.findUnresolvedAlerts(companyId);
    }

    /**
     * 获取最近的告警
     */
    public List<Alert> getRecentAlerts(Long companyId, int limit) {
        return alertRepository.findRecentAlerts(companyId, Pageable.ofSize(limit));
    }

    /**
     * 按类型获取告警
     */
    public List<Alert> getAlertsByType(Long companyId, String alertType) {
        return alertRepository.findByCompanyIdAndAlertType(companyId, alertType);
    }

    /**
     * 统计未解决的告警数量
     */
    public long countUnresolvedAlerts(Long companyId) {
        return alertRepository.countUnresolvedAlerts(companyId);
    }

    /**
     * 统计告警（按严重程度分组）
     */
    public Map<String, Long> getAlertStatistics(Long companyId) {
        List<Object[]> stats = alertRepository.countAlertsBySeverityGrouped(companyId);
        return java.util.stream.StreamSupport.stream(stats.spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    /**
     * 解决设备的离线告警（设备重新上线时调用）
     */
    @Transactional
    public void resolveOfflineAlerts(String deviceCode, Long deviceId) {
        List<Alert> unresolvedAlerts = alertRepository.findByDeviceIdAndResolved(deviceId, false)
                .stream()
                .filter(alert -> AlertType.OFFLINE.getCode().equals(alert.getAlertType()))
                .toList();

        if (!unresolvedAlerts.isEmpty()) {
            unresolvedAlerts.forEach(alert -> {
                alert.setResolved(true);
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
            });

            log.info("✅ 设备{}重新上线，解决{}个离线告警",
                     deviceCode, unresolvedAlerts.size());
        }
    }
}
