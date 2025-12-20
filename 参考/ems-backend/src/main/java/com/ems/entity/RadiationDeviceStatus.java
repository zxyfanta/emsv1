package com.ems.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import com.ems.entity.device.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 辐射设备状态记录实体
 * 存储辐射监测仪的MQTT数据，包含GPS定位和基础监测数据
 *
 * @author EMS Team
 */
@Entity
@Table(name = "radiation_device_status", indexes = {
    @Index(name = "idx_rad_device_time", columnList = "device_id, record_time"),
    @Index(name = "idx_rad_record_time", columnList = "record_time"),
    @Index(name = "idx_rad_cpm_value", columnList = "cpm_value"),
    @Index(name = "idx_rad_battery_voltage", columnList = "battery_voltage_mv"),
    @Index(name = "idx_rad_signal_quality", columnList = "signal_quality")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiationDeviceStatus {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联设备
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Device device;

    /**
     * 记录时间
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    // ==================== 核心监测数据====================

    /**
     * CPM辐射值
     * 每分钟计数，用于辐射监测
     */
    @Column(name = "cpm_value")
    private Integer cpmValue;

    /**
     * 电池电压（毫伏）
     * 例如：3989 表示 3.989V
     */
    @Column(name = "battery_voltage_mv")
    private Integer batteryVoltageMv;

    /**
     * 信号强度
     * 范围：1-5，5为最强
     */
    @Column(name = "signal_quality")
    private Integer signalQuality;

    /**
     * 设备温度（摄氏度）
     */
    @Column(name = "device_temperature")
    private Double deviceTemperature;

    // ==================== GPS位置数据====================

    /**
     * BDS北斗经度（度分格式）
     */
    @Column(name = "bds_longitude", length = 20)
    private String bdsLongitude;

    /**
     * BDS北斗纬度（度分格式）
     */
    @Column(name = "bds_latitude", length = 20)
    private String bdsLatitude;

    /**
     * BDS UTC时间
     */
    @Column(name = "bds_utc", length = 20)
    private String bdsUtc;

    /**
     * BDS定位是否有效
     */
    @Column(name = "bds_useful")
    private Boolean bdsUseful;

    /**
     * LBS基站定位经度（十进制格式）
     */
    @Column(name = "lbs_longitude")
    private Double lbsLongitude;

    /**
     * LBS基站定位纬度（十进制格式）
     */
    @Column(name = "lbs_latitude")
    private Double lbsLatitude;

    /**
     * LBS定位是否有效
     */
    @Column(name = "lbs_useful")
    private Boolean lbsUseful;

    // ==================== 时间信息====================

    /**
     * 设备本地时间字符串
     */
    @Column(name = "local_time_string", length = 50)
    private String localTimeString;

    /**
     * 数据创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== 业务方法====================

    /**
     * 获取电池电压（伏特）
     */
    public Double getBatteryVoltageVolts() {
        return batteryVoltageMv != null ? batteryVoltageMv / 1000.0 : null;
    }

    /**
     * 检查电池是否低电量
     * 辐射设备阈值：3.5V
     */
    public boolean isLowBattery() {
        Double voltage = getBatteryVoltageVolts();
        return voltage != null && voltage < 3.5;
    }

    /**
     * 检查辐射值是否异常
     * 辐射设备阈值：100
     */
    public boolean isHighCpm() {
        return cpmValue != null && cpmValue > 100;
    }

    /**
     * 检查设备温度是否异常
     * 正常范围：-20°C 到 +60°C
     */
    public boolean isAbnormalTemperature() {
        return deviceTemperature != null && (deviceTemperature < -20 || deviceTemperature > 60);
    }

    /**
     * 检查是否有有效的GPS定位
     */
    public boolean hasValidLocation() {
        return (bdsUseful != null && bdsUseful && bdsLongitude != null && bdsLatitude != null) ||
               (lbsUseful != null && lbsUseful && lbsLongitude != null && lbsLatitude != null);
    }

    /**
     * 获取主要定位类型
     */
    public String getPrimaryLocationType() {
        if (bdsUseful != null && bdsUseful) {
            return "BDS";
        } else if (lbsUseful != null && lbsUseful) {
            return "LBS";
        }
        return "NONE";
    }

    /**
     * 检查是否有任何异常数据
     */
    public boolean hasAnyAbnormalData() {
        return isLowBattery() || isHighCpm() || isAbnormalTemperature();
    }

    /**
     * 检查设备状态是否健康
     */
    public boolean isHealthy() {
        return !hasAnyAbnormalData() && signalQuality != null && signalQuality >= 2;
    }

    // ==================== 静态工厂方法====================

    /**
     * 从MQTT数据创建辐射设备状态记录
     */
    public static RadiationDeviceStatus createFromMqttData(Device device, String jsonData) {
        RadiationDeviceStatus status = RadiationDeviceStatus.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .build();

        // TODO: 解析JSON数据并设置相应字段
        // 这里需要根据实际的JSON格式进行解析

        return status;
    }

    /**
     * 创建错误记录
     */
    public static RadiationDeviceStatus createErrorRecord(Device device, String errorMessage) {
        return RadiationDeviceStatus.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .build();
    }
}