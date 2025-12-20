package com.ems.dto.aggregation;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图表数据点DTO
 * 用于前端图表展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 数据值
     */
    private Double value;

    /**
     * 数据来源类型
     * realtime: 实时数据
     * 5min: 5分钟聚合
     * hourly: 小时聚合
     * daily: 日聚合
     * weekly: 周聚合
     * monthly: 月聚合
     */
    private String sourceType;

    /**
     * 数据质量分数 (0-1)
     */
    private Double qualityScore;

    /**
     * 数据点标签（可选）
     */
    private String label;

    /**
     * 扩展信息（可选）
     */
    private Object metadata;

    public ChartDataPoint(LocalDateTime timestamp, Double value, String sourceType) {
        this.timestamp = timestamp;
        this.value = value;
        this.sourceType = sourceType;
    }

    /**
     * 获取时间戳的毫秒数（用于前端图表）
     */
    public long getTimestampMillis() {
        return timestamp != null ? timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;
    }

    /**
     * 判断数据点是否有效
     */
    public boolean isValid() {
        return timestamp != null && value != null && sourceType != null;
    }

    /**
     * 获取数据源类型的显示名称
     */
    public String getSourceTypeDisplayName() {
        switch (sourceType) {
            case "realtime":
                return "实时数据";
            case "5min":
                return "5分钟聚合";
            case "hourly":
                return "小时聚合";
            case "daily":
                return "日聚合";
            case "weekly":
                return "周聚合";
            case "monthly":
                return "月聚合";
            default:
                return sourceType;
        }
    }

    @Override
    public String toString() {
        return String.format("ChartDataPoint{time=%s, value=%.2f, source=%s}",
                timestamp, value, sourceType);
    }
}