package com.ems.entity;

import jakarta.persistence.*;
import com.ems.entity.device.Device;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 告警规则实体类
 * 定义设备告警的触发条件和处理规则
 *
 * @author EMS Team
 */
@Entity
@Table(name = "alert_rules", indexes = {
    @Index(name = "idx_alert_rule_device_id", columnList = "device_id"),
    @Index(name = "idx_alert_rule_enterprise_id", columnList = "enterprise_id"),
    @Index(name = "idx_alert_rule_enabled", columnList = "enabled"),
    @Index(name = "idx_alert_rule_created_at", columnList = "created_at"),
    @Index(name = "idx_alert_rule_deleted", columnList = "deleted")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联设备
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 规则名称
     */
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /**
     * 监控指标类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_name", nullable = false)
    private MetricName metricName;

    /**
     * 告警条件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private ConditionType conditionType;

    /**
     * 阈值范围 - 最小值
     */
    @Column(name = "threshold_min")
    private Double thresholdMin;

    /**
     * 阈值范围 - 最大值
     */
    @Column(name = "threshold_max")
    private Double thresholdMax;

    /**
     * 告警严重级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    /**
     * 冷却时间（分钟），防止重复告警
     */
    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes = 5;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 规则描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 是否删除（软删除）
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * 监控指标类型枚举
     */
    public enum MetricName {
        CPM("辐射计量值", "cpm"),
        BATTERY_VOLTAGE("电池电压", "V"),
        DEVICE_OFFLINE("设备离线", "status"),
        GPS_ABNORMAL("GPS位置异常", "location"),
        SIGNAL_STRENGTH("信号强度", "dBm");

        private final String description;
        private final String unit;

        MetricName(String description, String unit) {
            this.description = description;
            this.unit = unit;
        }

        public String getDescription() {
            return description;
        }

        public String getUnit() {
            return unit;
        }
    }

    /**
     * 告警条件类型枚举
     */
    public enum ConditionType {
        GREATER_THAN("大于", ">"),
        LESS_THAN("小于", "<"),
        GREATER_EQUAL("大于等于", ">="),
        LESS_EQUAL("小于等于", "<="),
        EQUAL("等于", "=="),
        NOT_EQUAL("不等于", "!="),
        BETWEEN("范围内", "BETWEEN"),
        OUTSIDE_RANGE("范围外", "NOT BETWEEN"),
        IS_NULL("为空", "IS NULL"),
        IS_NOT_NULL("不为空", "IS NOT NULL");

        private final String description;
        private final String operator;

        ConditionType(String description, String operator) {
            this.description = description;
            this.operator = operator;
        }

        public String getDescription() {
            return description;
        }

        public String getOperator() {
            return operator;
        }
    }

    /**
     * 告警严重级别枚举
     */
    public enum AlertSeverity {
        LOW("低", "info", "#28a745"),
        MEDIUM("中", "warning", "#ffc107"),
        HIGH("高", "error", "#fd7e14"),
        CRITICAL("严重", "critical", "#dc3545");

        private final String description;
        private final String level;
        private final String color;

        AlertSeverity(String description, String level, String color) {
            this.description = description;
            this.level = level;
            this.color = color;
        }

        public String getDescription() {
            return description;
        }

        public String getLevel() {
            return level;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * 检查是否触发告警
     */
    public boolean isTriggered(Double triggerValue) {
        if (!enabled || deleted) {
            return false;
        }

        switch (conditionType) {
            case GREATER_THAN:
                return triggerValue != null && triggerValue > thresholdMax;
            case LESS_THAN:
                return triggerValue != null && triggerValue < thresholdMin;
            case GREATER_EQUAL:
                return triggerValue != null && triggerValue >= thresholdMax;
            case LESS_EQUAL:
                return triggerValue != null && triggerValue <= thresholdMin;
            case EQUAL:
                return triggerValue != null && triggerValue.equals(thresholdMax);
            case NOT_EQUAL:
                return triggerValue != null && !triggerValue.equals(thresholdMax);
            case BETWEEN:
                return triggerValue != null &&
                       thresholdMin != null && thresholdMax != null &&
                       triggerValue >= thresholdMin && triggerValue <= thresholdMax;
            case OUTSIDE_RANGE:
                return triggerValue != null &&
                       thresholdMin != null && thresholdMax != null &&
                       (triggerValue < thresholdMin || triggerValue > thresholdMax);
            case IS_NULL:
                return triggerValue == null;
            case IS_NOT_NULL:
                return triggerValue != null;
            default:
                return false;
        }
    }

    /**
     * 生成告警消息
     */
    public String getAlertMessage(Double triggerValue) {
        StringBuilder message = new StringBuilder();
        message.append("告警触发：").append(ruleName);
        message.append(" - 设备：").append(device != null ? device.getDeviceName() : "未知");
        message.append("，指标：").append(metricName.getDescription());
        message.append("，当前值：").append(triggerValue);

        if (thresholdMin != null && thresholdMax != null) {
            message.append("，阈值范围：[").append(thresholdMin).append(", ").append(thresholdMax).append("]");
        } else if (thresholdMax != null) {
            message.append("，阈值：").append(thresholdMax);
        } else if (thresholdMin != null) {
            message.append("，阈值：").append(thresholdMin);
        }

        message.append("，级别：").append(severity.getDescription());
        return message.toString();
    }

    /**
     * 创建CPM高值告警规则
     */
    public static AlertRule createCpmHighRule(Device device, Double threshold) {
        AlertRule rule = new AlertRule();
        rule.setDevice(device);
        rule.setRuleName("CPM辐射高值告警");
        rule.setMetricName(MetricName.CPM);
        rule.setConditionType(ConditionType.GREATER_THAN);
        rule.setThresholdMax(threshold);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setCooldownMinutes(10);
        rule.setDescription(String.format("当CPM值超过%s时触发告警", threshold));
        rule.setEnabled(true);
        return rule;
    }

    /**
     * 创建电池低电量告警规则
     */
    public static AlertRule createBatteryLowRule(Device device, Double threshold) {
        AlertRule rule = new AlertRule();
        rule.setDevice(device);
        rule.setRuleName("电池低电量告警");
        rule.setMetricName(MetricName.BATTERY_VOLTAGE);
        rule.setConditionType(ConditionType.LESS_THAN);
        rule.setThresholdMin(threshold);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setCooldownMinutes(30);
        rule.setDescription(String.format("当电池电压低于%sV时触发告警", threshold));
        rule.setEnabled(true);
        return rule;
    }
}