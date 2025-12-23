package com.cdutetc.ems.service;

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
    private final ObjectMapper objectMapper;

    // 告警阈值配置
    private static final double HIGH_CPM_THRESHOLD = 100.0;  // 高辐射值阈值
    private static final double LOW_BATTERY_THRESHOLD = 3.5;  // 低电量阈值 (V)

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
     * 检查辐射数据并触发告警
     */
    public void checkRadiationDataAndAlert(String deviceCode, Double cpm, Long deviceId, Long companyId) {
        // 检查高辐射值
        if (cpm != null && cpm > HIGH_CPM_THRESHOLD) {
            createAlert(
                AlertType.HIGH_CPM,
                AlertSeverity.CRITICAL,
                deviceCode,
                deviceId,
                companyId,
                String.format("辐射值超标: 当前值 %.2f CPM，阈值 %d CPM", cpm, (int) HIGH_CPM_THRESHOLD),
                Map.of("cpm", cpm, "threshold", HIGH_CPM_THRESHOLD)
            );
        }
    }

    /**
     * 检查环境数据并触发告警
     */
    public void checkEnvironmentDataAndAlert(String deviceCode, Double battery, Long deviceId, Long companyId) {
        // 检查低电量
        if (battery != null && battery < LOW_BATTERY_THRESHOLD) {
            createAlert(
                AlertType.LOW_BATTERY,
                AlertSeverity.WARNING,
                deviceCode,
                deviceId,
                companyId,
                String.format("电量不足: 当前电压 %.2f V，阈值 %.1f V", battery, LOW_BATTERY_THRESHOLD),
                Map.of("battery", battery, "threshold", LOW_BATTERY_THRESHOLD)
            );
        }
    }

    /**
     * 检查设备状态并触发告警
     */
    public void checkDeviceStatusAndAlert(Device device) {
        // 检查设备故障
        if (DeviceStatus.FAULT.name().equals(device.getStatus())) {
            createAlert(
                AlertType.FAULT,
                AlertSeverity.CRITICAL,
                device.getDeviceCode(),
                device.getId(),
                device.getCompany().getId(),
                "设备故障: 设备状态异常",
                Map.of("deviceStatus", device.getStatus())
            );
        }

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
     */
    @Transactional
    public int resolveAlertsByDevice(Long deviceId, Long companyId) {
        List<Alert> alerts = alertRepository.findByDeviceId(deviceId);
        int count = 0;

        for (Alert alert : alerts) {
            if (!alert.getResolved() && alert.getCompany().getId().equals(companyId)) {
                alert.setResolved(true);
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
                count++;
            }
        }

        log.info("批量解决告警: deviceId={}, count={}", deviceId, count);
        return count;
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
}
