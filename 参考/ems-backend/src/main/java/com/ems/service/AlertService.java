package com.ems.service;

import com.ems.entity.AlertRecord;
import com.ems.entity.AlertRule;
import com.ems.entity.device.Device;
import com.ems.repository.AlertRecordRepository;
import com.ems.repository.AlertRuleRepository;
import com.ems.repository.device.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警服务
 * 负责告警规则管理和告警检测
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 创建告警规则
     */
    @Transactional
    public AlertRule createAlertRule(AlertRule alertRule) {
        try {
            // 验证设备是否存在
            Device device = deviceRepository.findByDeviceId(alertRule.getDevice().getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + alertRule.getDevice().getDeviceId()));

            alertRule.setDevice(device);
            alertRule.setCreatedAt(LocalDateTime.now());
            alertRule.setUpdatedAt(LocalDateTime.now());
            alertRule.setDeleted(false);

            AlertRule savedRule = alertRuleRepository.save(alertRule);
            log.info("创建告警规则成功: 设备={}, 指标={}",
                savedRule.getDevice().getDeviceId(), savedRule.getMetricName());

            return savedRule;
        } catch (Exception e) {
            log.error("创建告警规则失败", e);
            throw new RuntimeException("创建告警规则失败", e);
        }
    }

    /**
     * 更新告警规则
     */
    @Transactional
    public AlertRule updateAlertRule(Long id, AlertRule alertRule) {
        try {
            AlertRule existingRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));

            // 更新允许修改的字段
            existingRule.setRuleName(alertRule.getRuleName());
            existingRule.setConditionType(alertRule.getConditionType());
            existingRule.setThresholdMax(alertRule.getThresholdMax());
            existingRule.setThresholdMin(alertRule.getThresholdMin());
            existingRule.setSeverity(alertRule.getSeverity());
            existingRule.setCooldownMinutes(alertRule.getCooldownMinutes());
            existingRule.setEnabled(alertRule.getEnabled());
            existingRule.setDescription(alertRule.getDescription());
            existingRule.setUpdatedAt(LocalDateTime.now());

            AlertRule savedRule = alertRuleRepository.save(existingRule);
            log.info("更新告警规则成功: ID={}", id);

            return savedRule;
        } catch (Exception e) {
            log.error("更新告警规则失败，ID: {}", id, e);
            throw new RuntimeException("更新告警规则失败", e);
        }
    }

    /**
     * 删除告警规则（软删除）
     */
    @Transactional
    public void deleteAlertRule(Long id) {
        try {
            AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));

            alertRule.setDeleted(true);
            alertRule.setUpdatedAt(LocalDateTime.now());
            alertRuleRepository.save(alertRule);

            log.info("删除告警规则成功: ID={}", id);
        } catch (Exception e) {
            log.error("删除告警规则失败，ID: {}", id, e);
            throw new RuntimeException("删除告警规则失败", e);
        }
    }

    /**
     * 检查告警触发（简化版）
     */
    public void checkAlerts(String deviceId, Double cpmValue, Integer batteryVoltage, LocalDateTime timestamp) {
        try {
            List<AlertRule> enabledRules = alertRuleRepository.findByDeviceIdAndEnabled(deviceId, true);

            // 1. 首先检查告警恢复
            checkAlertRecovery(deviceId, enabledRules, cpmValue, batteryVoltage, timestamp);

            // 2. 然后检查新的告警触发
            for (AlertRule rule : enabledRules) {
                checkSingleAlert(deviceId, rule, cpmValue, batteryVoltage, timestamp);
            }
        } catch (Exception e) {
            log.error("检查告警失败，设备ID: {}", deviceId, e);
        }
    }

    /**
     * 检查单个告警规则
     */
    private void checkSingleAlert(String deviceId, AlertRule rule, Double cpmValue, Integer batteryVoltage, LocalDateTime timestamp) {
        try {
            Double triggerValue = getTriggerValue(rule.getMetricName(), cpmValue, batteryVoltage);
            if (triggerValue == null) {
                return; // 没有对应的触发值
            }

            // 检查是否触发告警
            if (rule.isTriggered(triggerValue)) {
                triggerAlert(deviceId, rule, triggerValue, timestamp);
            }
        } catch (Exception e) {
            log.error("检查单个告警规则失败，设备ID: {}, 规则ID: {}", deviceId, rule.getId(), e);
        }
    }

    /**
     * 触发告警（简化版）
     */
    private void triggerAlert(String deviceId, AlertRule rule, Double triggerValue, LocalDateTime timestamp) {
        try {
            Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);
            if (device == null) {
                log.warn("设备不存在，无法创建告警记录: {}", deviceId);
                return;
            }

            // 检查是否已存在相同的活跃告警（在冷却时间内）
            List<AlertRecord> existingAlerts = alertRecordRepository.findByDeviceIdAndStatus(
                deviceId, AlertRecord.AlertStatus.ACTIVE);

            for (AlertRecord existing : existingAlerts) {
                if (existing.getRule().getId().equals(rule.getId())) {
                    // 检查是否在冷却时间内
                    LocalDateTime cooldownThreshold = existing.getCreatedAt().plusMinutes(rule.getCooldownMinutes());
                    if (LocalDateTime.now().isBefore(cooldownThreshold)) {
                        log.debug("设备 {} 告警规则 {} 在冷却时间内，跳过创建", deviceId, rule.getRuleName());
                        return;
                    }
                }
            }

            // 创建告警记录
            AlertRecord alertRecord = AlertRecord.create(
                device, rule, triggerValue, rule.getThresholdMax() != null ? rule.getThresholdMax() : rule.getThresholdMin(),
                rule.getAlertMessage(triggerValue)
            );

            alertRecordRepository.save(alertRecord);

            log.warn("触发告警: 设备={}, 指标={}, 触发值={}, 严重级别={}",
                deviceId, rule.getMetricName(), triggerValue, rule.getSeverity());

        } catch (Exception e) {
            log.error("触发告警失败", e);
        }
    }

    /**
     * 确认告警
     */
    @Transactional
    public void acknowledgeAlert(Long alertId, String acknowledgedBy, String notes) {
        try {
            AlertRecord alert = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + alertId));

            alert.acknowledge(acknowledgedBy, notes);
            alertRecordRepository.save(alert);

            log.info("确认告警成功: ID={}, 确认人={}", alertId, acknowledgedBy);
        } catch (Exception e) {
            log.error("确认告警失败，ID: {}", alertId, e);
            throw new RuntimeException("确认告警失败", e);
        }
    }

    /**
     * 解决告警
     */
    @Transactional
    public void resolveAlert(Long alertId, String notes) {
        try {
            AlertRecord alert = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + alertId));

            alert.resolveDirectly(notes);
            alertRecordRepository.save(alert);

            log.info("解决告警成功: ID={}", alertId);
        } catch (Exception e) {
            log.error("解决告警失败，ID: {}", alertId, e);
            throw new RuntimeException("解决告警失败", e);
        }
    }

    /**
     * 获取设备告警规则列表
     */
    public List<AlertRule> getDeviceAlertRules(String deviceId) {
        try {
            return alertRuleRepository.findByDeviceDeviceId(deviceId);
        } catch (Exception e) {
            log.error("获取设备告警规则失败，设备ID: {}", deviceId, e);
            return List.of();
        }
    }

    /**
     * 获取设备告警记录列表
     */
    public List<AlertRecord> getDeviceAlertRecords(String deviceId) {
        try {
            return alertRecordRepository.findByDeviceId(deviceId);
        } catch (Exception e) {
            log.error("获取设备告警记录失败，设备ID: {}", deviceId, e);
            return List.of();
        }
    }

    /**
     * 获取活跃告警列表
     */
    public List<AlertRecord> getActiveAlerts() {
        try {
            return alertRecordRepository.findActiveAlerts();
        } catch (Exception e) {
            log.error("获取活跃告警列表失败", e);
            return List.of();
        }
    }

    /**
     * 检查告警恢复机制
     * 当数据恢复正常时，自动解决活跃告警
     */
    @Transactional
    public void checkAlertRecovery(String deviceId, List<AlertRule> enabledRules,
                                  Double cpmValue, Integer batteryVoltage, LocalDateTime timestamp) {
        try {
            // 获取该设备的所有活跃告警
            List<AlertRecord> activeAlerts = alertRecordRepository.findByDeviceIdAndStatus(
                deviceId, AlertRecord.AlertStatus.ACTIVE);

            for (AlertRecord activeAlert : activeAlerts) {
                AlertRule rule = activeAlert.getRule();

                // 如果规则已被禁用，自动解决告警
                if (!enabledRules.contains(rule)) {
                    activeAlert.resolveAuto("规则已禁用");
                    alertRecordRepository.save(activeAlert);
                    log.info("自动解决告警: 设备={}, 规则已禁用, 告警ID={}", deviceId, activeAlert.getId());
                    continue;
                }

                // 获取当前值进行恢复检测
                Double currentValue = getTriggerValue(rule.getMetricName(), cpmValue, batteryVoltage);

                if (currentValue != null) {
                    // 检查当前值是否已恢复到正常范围
                    if (!rule.isTriggered(currentValue)) {
                        activeAlert.resolveAuto(String.format("数据恢复正常: %s = %.2f",
                            rule.getMetricName().getDescription(), currentValue));
                        alertRecordRepository.save(activeAlert);

                        log.info("自动解决告警: 设备={}, 指标={}, 当前值={}, 告警ID={}",
                            deviceId, rule.getMetricName().getDescription(), currentValue, activeAlert.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查告警恢复失败，设备ID: {}", deviceId, e);
        }
    }

    /**
     * 根据指标名称获取触发值
     */
    private Double getTriggerValue(AlertRule.MetricName metricName, Double cpmValue, Integer batteryVoltage) {
        switch (metricName) {
            case CPM:
                return cpmValue;
            case BATTERY_VOLTAGE:
                return batteryVoltage != null ? batteryVoltage.doubleValue() : null;
            default:
                return null;
        }
    }

    /**
     * 初始化设备默认告警规则
     */
    @Transactional
    public void initializeDefaultAlertRules(Device device) {
        try {
            // 验证设备有效性
            if (device == null || device.getDeviceId() == null) {
                log.warn("设备信息无效，跳过告警规则初始化");
                return;
            }

            // 检查是否已存在告警规则
            List<AlertRule> existingRules = alertRuleRepository.findByDeviceIdAndEnabled(
                device.getDeviceId(), true);

            if (!existingRules.isEmpty()) {
                log.info("设备 {} 已存在 {} 条告警规则，跳过初始化", device.getDeviceId(), existingRules.size());
                return;
            }

            // 确保设备已正确保存（有ID）
            if (device.getId() == null) {
                log.warn("设备未正确保存到数据库，跳过告警规则初始化: {}", device.getDeviceId());
                return;
            }

            // 创建CPM高值告警规则
            try {
                AlertRule cpmRule = AlertRule.createCpmHighRule(device, 100.0);
                alertRuleRepository.save(cpmRule);
                log.info("创建CPM高值告警规则成功: 设备={}, 规则ID={}", device.getDeviceId(), cpmRule.getId());
            } catch (Exception e) {
                log.error("创建CPM高值告警规则失败: 设备={}", device.getDeviceId(), e);
                // 继续创建其他规则
            }

            // 创建电池低电量告警规则
            try {
                AlertRule batteryRule = AlertRule.createBatteryLowRule(device, 3.7);
                alertRuleRepository.save(batteryRule);
                log.info("创建电池低电量告警规则成功: 设备={}, 规则ID={}", device.getDeviceId(), batteryRule.getId());
            } catch (Exception e) {
                log.error("创建电池低电量告警规则失败: 设备={}", device.getDeviceId(), e);
            }

            log.info("为设备 {} 初始化默认告警规则完成", device.getDeviceId());

        } catch (Exception e) {
            log.error("初始化设备 {} 默认告警规则失败", device != null ? device.getDeviceId() : "null", e);
            // 不抛出异常，避免影响设备创建主流程
        }
    }

    /**
     * 检查设备离线告警
     */
    public void checkDeviceOfflineAlert(Device device, LocalDateTime lastSeenTime) {
        try {
            // 检查是否存在活跃的离线告警
            List<AlertRecord> existingOfflineAlerts = alertRecordRepository
                .findByDeviceIdAndStatus(device.getDeviceId(), AlertRecord.AlertStatus.ACTIVE)
                .stream()
                .filter(alert -> alert.getTitle().contains("离线"))
                .toList();

            if (!existingOfflineAlerts.isEmpty()) {
                log.debug("设备 {} 已存在活跃的离线告警", device.getDeviceId());
                return;
            }

            // 创建离线告警记录
            AlertRecord offlineAlert = AlertRecord.createDeviceOfflineAlert(device, lastSeenTime);
            alertRecordRepository.save(offlineAlert);

            log.warn("设备 {} 触发离线告警，最后在线时间: {}", device.getDeviceId(), lastSeenTime);

        } catch (Exception e) {
            log.error("检查设备离线告警失败，设备ID: {}", device.getDeviceId(), e);
        }
    }
}