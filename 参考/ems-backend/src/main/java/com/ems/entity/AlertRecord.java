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
 * 告警记录实体类
 * 记录设备触发的告警信息和处理状态
 *
 * @author EMS Team
 */
@Entity
@Table(name = "alert_records", indexes = {
    @Index(name = "idx_alert_record_device_id", columnList = "device_id"),
    @Index(name = "idx_alert_record_rule_id", columnList = "rule_id"),
    @Index(name = "idx_alert_record_status", columnList = "status"),
    @Index(name = "idx_alert_record_severity", columnList = "severity"),
    @Index(name = "idx_alert_record_triggered_at", columnList = "triggered_at"),
    @Index(name = "idx_alert_record_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class AlertRecord {

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
     * 关联告警规则
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule rule;

    /**
     * 告警标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 告警消息
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 触发值
     */
    @Column(name = "trigger_value")
    private Double triggerValue;

    /**
     * 阈值
     */
    @Column(name = "threshold_value")
    private Double thresholdValue;

    /**
     * 告警严重级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertRule.AlertSeverity severity;

    /**
     * 告警状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status = AlertStatus.ACTIVE;

    /**
     * 触发时间（数据时间戳）
     */
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    /**
     * 确认时间
     */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /**
     * 确认人
     */
    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    /**
     * 确认备注
     */
    @Column(name = "acknowledgment_notes", length = 1000)
    private String acknowledgmentNotes;

    /**
     * 解决时间
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * 解决备注
     */
    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    /**
     * 处理时长（分钟）
     */
    @Column(name = "resolution_duration_minutes")
    private Integer resolutionDurationMinutes;

    /**
     * 是否已发送通知
     */
    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    /**
     * 通知发送时间
     */
    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

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
     * 告警状态枚举
     */
    public enum AlertStatus {
        ACTIVE("活跃", "active", "#dc3545"),
        ACKNOWLEDGED("已确认", "acknowledged", "#ffc107"),
        RESOLVED("已解决", "resolved", "#28a745"),
        SUPPRESSED("已抑制", "suppressed", "#6c757d");

        private final String description;
        private final String status;
        private final String color;

        AlertStatus(String description, String status, String color) {
            this.description = description;
            this.status = status;
            this.color = color;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * 确认告警
     */
    public void acknowledge(String acknowledgedBy, String notes) {
        if (status == AlertStatus.ACTIVE) {
            this.status = AlertStatus.ACKNOWLEDGED;
            this.acknowledgedAt = LocalDateTime.now();
            this.acknowledgedBy = acknowledgedBy;
            this.acknowledgmentNotes = notes;
        }
    }

    /**
     * 解决告警
     */
    public void resolve(String notes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;

        // 计算处理时长
        if (triggeredAt != null) {
            this.resolutionDurationMinutes = (int) java.time.Duration.between(
                triggeredAt, resolvedAt).toMinutes();
        }
    }

    /**
     * 直接解决告警（从活跃状态）
     */
    public void resolveDirectly(String notes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;

        // 计算处理时长
        if (triggeredAt != null) {
            this.resolutionDurationMinutes = (int) java.time.Duration.between(
                triggeredAt, resolvedAt).toMinutes();
        }
    }

    /**
     * 自动解决告警（数据恢复正常）
     */
    public void resolveAuto(String reason) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = "系统自动解决：" + reason;

        // 计算处理时长
        if (triggeredAt != null) {
            this.resolutionDurationMinutes = (int) java.time.Duration.between(
                triggeredAt, resolvedAt).toMinutes();
        }
    }

    /**
     * 抑制告警
     */
    public void suppress() {
        this.status = AlertStatus.SUPPRESSED;
    }

    /**
     * 标记通知已发送
     */
    public void markNotificationSent() {
        this.notificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
    }

    /**
     * 检查告警是否活跃（未解决）
     */
    public boolean isActive() {
        return status == AlertStatus.ACTIVE || status == AlertStatus.ACKNOWLEDGED;
    }

    /**
     * 检查告警是否已确认
     */
    public boolean isAcknowledged() {
        return status == AlertStatus.ACKNOWLEDGED;
    }

    /**
     * 检查告警是否已解决
     */
    public boolean isResolved() {
        return status == AlertStatus.RESOLVED;
    }

    /**
     * 获取告警持续时间（分钟）
     */
    public Integer getDurationMinutes() {
        LocalDateTime endTime = resolvedAt != null ? resolvedAt : LocalDateTime.now();
        return triggeredAt != null ? (int) java.time.Duration.between(triggeredAt, endTime).toMinutes() : null;
    }

    /**
     * 创建告警记录
     */
    public static AlertRecord create(Device device, AlertRule rule, Double triggerValue,
                                   Double thresholdValue, String message) {
        AlertRecord record = new AlertRecord();
        record.setDevice(device);
        record.setRule(rule);
        record.setTitle(rule.getRuleName());
        record.setMessage(message);
        record.setTriggerValue(triggerValue);
        record.setThresholdValue(thresholdValue);
        record.setSeverity(rule.getSeverity());
        record.setStatus(AlertStatus.ACTIVE);
        record.setTriggeredAt(LocalDateTime.now());
        record.setNotificationSent(false);
        return record;
    }

    /**
     * 创建设备离线告警
     */
    public static AlertRecord createDeviceOfflineAlert(Device device, LocalDateTime offlineSince) {
        AlertRecord record = new AlertRecord();
        record.setDevice(device);
        record.setTitle("设备离线告警");
        record.setMessage(String.format("设备 %s 已离线，离线时间：%s",
                         device.getDeviceName(), offlineSince));
        record.setTriggerValue(null);
        record.setThresholdValue(null);
        record.setSeverity(AlertRule.AlertSeverity.MEDIUM);
        record.setStatus(AlertStatus.ACTIVE);
        record.setTriggeredAt(offlineSince);
        record.setNotificationSent(false);
        return record;
    }

    /**
     * 创建GPS位置异常告警
     */
    public static AlertRecord createGpsAbnormalAlert(Device device, String reason) {
        AlertRecord record = new AlertRecord();
        record.setDevice(device);
        record.setTitle("GPS位置异常告警");
        record.setMessage(String.format("设备 %s GPS位置异常：%s",
                         device.getDeviceName(), reason));
        record.setTriggerValue(null);
        record.setThresholdValue(null);
        record.setSeverity(AlertRule.AlertSeverity.LOW);
        record.setStatus(AlertStatus.ACTIVE);
        record.setTriggeredAt(LocalDateTime.now());
        record.setNotificationSent(false);
        return record;
    }
}