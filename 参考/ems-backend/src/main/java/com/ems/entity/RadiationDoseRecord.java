package com.ems.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ems.entity.device.Device;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 辐射剂量记录实体类
 * 用于存储累积剂量和剂量率计算结果
 *
 * @author EMS Team
 */
@Entity
@Table(name = "radiation_dose_records", indexes = {
    @Index(name = "idx_dose_device_id", columnList = "device_id"),
    @Index(name = "idx_dose_record_time", columnList = "record_time"),
    @Index(name = "idx_dose_calculation_type", columnList = "calculation_type"),
    @Index(name = "idx_dose_device_time", columnList = "device_id,record_time"),
    @Index(name = "idx_dose_threshold_exceeded", columnList = "threshold_exceeded")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadiationDoseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的设备
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 记录时间
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    /**
     * 计算类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 50)
    private CalculationType calculationType;

    /**
     * 时间窗口开始时间
     */
    @Column(name = "window_start_time", nullable = false)
    private LocalDateTime windowStartTime;

    /**
     * 时间窗口结束时间
     */
    @Column(name = "window_end_time", nullable = false)
    private LocalDateTime windowEndTime;

    /**
     * 平均CPM值
     */
    @Column(name = "average_cpm", precision = 10, scale = 2)
    private BigDecimal averageCpm;

    /**
     * 最大CPM值
     */
    @Column(name = "max_cpm", precision = 10, scale = 2)
    private BigDecimal maxCpm;

    /**
     * 最小CPM值
     */
    @Column(name = "min_cpm", precision = 10, scale = 2)
    private BigDecimal minCpm;

    /**
     * 累积剂量 (μSv)
     */
    @Column(name = "cumulative_dose", precision = 15, scale = 6)
    private BigDecimal cumulativeDose;

    /**
     * 剂量率 (μSv/h)
     */
    @Column(name = "dose_rate", precision = 10, scale = 4)
    private BigDecimal doseRate;

    /**
     * 有效剂量 (μSv) - 考虑辐射类型和能量
     */
    @Column(name = "effective_dose", precision = 15, scale = 6)
    private BigDecimal effectiveDose;

    /**
     * 剂量当量 (μSv)
     */
    @Column(name = "dose_equivalent", precision = 15, scale = 6)
    private BigDecimal doseEquivalent;

    /**
     * 数据点数量
     */
    @Column(name = "data_points_count")
    private Integer dataPointsCount;

    /**
     * 计算参数（JSON格式）
     */
    @Column(name = "calculation_params", columnDefinition = "TEXT")
    private String calculationParams;

    /**
     * 校正因子
     */
    @Column(name = "correction_factor", precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal correctionFactor = BigDecimal.ONE;

    /**
     * 辐射类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "radiation_type", length = 50)
    private RadiationType radiationType;

    /**
     * 是否超过阈值
     */
    @Column(name = "threshold_exceeded", nullable = false)
    @Builder.Default
    private Boolean thresholdExceeded = false;

    /**
     * 超过的阈值类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_type", length = 50)
    private ThresholdType thresholdType;

    /**
     * 阈值值
     */
    @Column(name = "threshold_value", precision = 10, scale = 4)
    private BigDecimal thresholdValue;

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
     * 计算类型枚举
     */
    public enum CalculationType {
        HOURLY("小时剂量"),
        DAILY("日剂量"),
        WEEKLY("周剂量"),
        MONTHLY("月剂量"),
        YEARLY("年剂量"),
        REALTIME("实时剂量率"),
        CUMULATIVE("累积剂量"),
        STATISTICAL("统计分析");

        private final String description;

        CalculationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 辐射类型枚举
     */
    public enum RadiationType {
        GAMMA("伽马射线"),
        BETA("贝塔射线"),
        ALPHA("阿尔法射线"),
        NEUTRON("中子"),
        XRAY("X射线"),
        MIXED("混合辐射");

        private final String description;

        RadiationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 获取辐射权重因子
         */
        public BigDecimal getWeightFactor() {
            switch (this) {
                case GAMMA:
                case XRAY:
                    return BigDecimal.ONE;
                case BETA:
                    return BigDecimal.ONE;
                case ALPHA:
                    return new BigDecimal("20");
                case NEUTRON:
                    return new BigDecimal("10");
                case MIXED:
                    return new BigDecimal("5");
                default:
                    return BigDecimal.ONE;
            }
        }
    }

    /**
     * 阈值类型枚举
     */
    public enum ThresholdType {
        DOSE_RATE("剂量率阈值"),
        CUMULATIVE_DOSE("累积剂量阈值"),
        CPM_VALUE("CPM值阈值"),
        MAXIMUM_LIMIT("最大限值"),
        WARNING_LEVEL("警告级别"),
        DANGER_LEVEL("危险级别");

        private final String description;

        ThresholdType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查是否超过阈值
     */
    public boolean isThresholdExceeded() {
        return Boolean.TRUE.equals(thresholdExceeded);
    }

    /**
     * 检查数据有效性
     */
    public boolean isValid() {
        return device != null && recordTime != null && calculationType != null &&
               dataPointsCount != null && dataPointsCount > 0;
    }

    /**
     * 计算时间窗口长度（分钟）
     */
    public Long getWindowDurationMinutes() {
        if (windowStartTime != null && windowEndTime != null) {
            return java.time.Duration.between(windowStartTime, windowEndTime).toMinutes();
        }
        return null;
    }

    /**
     * 获取CPM范围
     */
    public String getCpmRange() {
        if (minCpm != null && maxCpm != null) {
            return String.format("%.2f - %.2f", minCpm, maxCpm);
        } else if (averageCpm != null) {
            return String.format("平均值: %.2f", averageCpm);
        }
        return "无数据";
    }

    /**
     * 获取剂量水平评估
     */
    public String getDoseLevel() {
        if (cumulativeDose == null) {
            return "未知";
        }

        BigDecimal dose = cumulativeDose;
        if (dose.compareTo(BigDecimal.ZERO) < 0) {
            return "异常";
        } else if (dose.compareTo(new BigDecimal("0.1")) <= 0) {
            return "正常";
        } else if (dose.compareTo(new BigDecimal("1.0")) <= 0) {
            return "轻度升高";
        } else if (dose.compareTo(new BigDecimal("10.0")) <= 0) {
            return "中度升高";
        } else {
            return "高度升高";
        }
    }

    /**
     * 获取风险等级
     */
    public String getRiskLevel() {
        if (!isThresholdExceeded()) {
            return "低";
        }

        if (thresholdType != null) {
            switch (thresholdType) {
                case WARNING_LEVEL:
                    return "中";
                case DANGER_LEVEL:
                case MAXIMUM_LIMIT:
                    return "高";
                default:
                    return "中";
            }
        }

        return "中";
    }

    /**
     * 获取设备ID
     */
    public String getDeviceId() {
        return device != null ? device.getDeviceId() : null;
    }

    /**
     * 计算剂量当量
     * 剂量当量 = 剂量 × 辐射权重因子 × 校正因子
     */
    public BigDecimal calculateDoseEquivalent() {
        if (cumulativeDose == null) {
            return null;
        }

        BigDecimal weightFactor = radiationType != null ?
                radiationType.getWeightFactor() : BigDecimal.ONE;

        return cumulativeDose.multiply(weightFactor)
                .multiply(correctionFactor != null ? correctionFactor : BigDecimal.ONE);
    }

    /**
     * 更新剂量当量
     */
    public void updateDoseEquivalent() {
        this.doseEquivalent = calculateDoseEquivalent();
    }

    /**
     * 检查是否为实时计算
     */
    public boolean isRealtimeCalculation() {
        return CalculationType.REALTIME.equals(calculationType);
    }

    /**
     * 检查是否为累积计算
     */
    public boolean isCumulativeCalculation() {
        return CalculationType.CUMULATIVE.equals(calculationType);
    }
}