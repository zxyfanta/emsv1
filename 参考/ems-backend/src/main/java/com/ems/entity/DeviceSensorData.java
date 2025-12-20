package com.ems.entity;

import jakarta.persistence.*;
import com.ems.entity.device.Device;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 设备传感器数据原始实体
 * 用于存储设备上报的原始传感器数据
 */
@Entity
@Table(name = "device_sensor_data", indexes = {
    @Index(name = "idx_device_time", columnList = "device_id,recorded_at"),
    @Index(name = "idx_metric_time", columnList = "metric_name,recorded_at"),
    @Index(name = "idx_device_metric", columnList = "device_id,metric_name"),
    @Index(name = "idx_device_metric_time", columnList = "device_id,metric_name,recorded_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, insertable = false, updatable = false)
    private Device device;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "metric_name", nullable = false, length = 50)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private Double value;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 指标名称枚举
     */
    public enum MetricName {
        CPM("CPM", "辐射计数"),
        BATVOLT("Batvolt", "电池电压");

        private final String code;
        private final String description;

        MetricName(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static MetricName fromCode(String code) {
            for (MetricName metric : values()) {
                if (metric.getCode().equals(code)) {
                    return metric;
                }
            }
            throw new IllegalArgumentException("Unknown metric code: " + code);
        }
    }

    /**
     * 判断数据是否有效
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.isBlank() &&
               metricName != null && !metricName.isBlank() &&
               value != null && !Double.isNaN(value) && !Double.isInfinite(value) &&
               recordedAt != null;
    }

    /**
     * 判断是否为异常值（基于业务规则）
     */
    public boolean isAnomalous() {
        if (value == null) {
            return true;
        }

        // CPM异常值判断
        if ("CPM".equals(metricName)) {
            return value < 0 || value > 100000; // CPM应该在合理范围内
        }

        // 电压异常值判断
        if ("Batvolt".equals(metricName)) {
            return value < 2000 || value > 5000; // 电压应该在2000-5000mV范围内
        }

        return false;
    }

    /**
     * 获取指标的单位
     */
    public String getMetricUnit() {
        if (unit != null) {
            return unit;
        }

        switch (metricName) {
            case "CPM":
                return "cpm";
            case "Batvolt":
                return "mV";
            default:
                return "";
        }
    }

    /**
     * 获取指标显示名称
     */
    public String getMetricDisplayName() {
        try {
            MetricName metric = MetricName.fromCode(metricName);
            return metric.getDescription();
        } catch (IllegalArgumentException e) {
            return metricName;
        }
    }

    /**
     * 获取企业ID（通过设备关联）
     *
     * @return enterprise ID or null if device is not loaded
     */
    public Long getEnterpriseId() {
        return device != null && device.getEnterprise() != null
            ? device.getEnterprise().getId()
            : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceSensorData that = (DeviceSensorData) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("DeviceSensorData{id=%d, device='%s', metric='%s', value=%.2f, time=%s}",
                id, deviceId, metricName, value, recordedAt);
    }
}