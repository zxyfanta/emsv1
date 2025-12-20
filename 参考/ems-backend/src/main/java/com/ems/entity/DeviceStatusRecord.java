package com.ems.entity;

import jakarta.persistence.*;
import com.ems.entity.device.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 设备状态记录实体
 * 存储设备的各种状态数据，如CPM值、电池电压、信号强度等
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Entity
@Table(name = "device_status_records", indexes = {
    @Index(name = "idx_device_record_time", columnList = "device_id, record_time"),
    @Index(name = "idx_record_time", columnList = "record_time"),
    @Index(name = "idx_cpm_value", columnList = "cpm_value"),
    @Index(name = "idx_battery_voltage", columnList = "battery_voltage_mv")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusRecord {

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
    private Device device;

    /**
     * 记录时间
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    // ==================== 核心状态数据====================

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

    // ==================== 扩展状态数据====================

    /**
     * 设备温度（摄氏度）
     */
    @Column(name = "device_temperature")
    private Double deviceTemperature;

    /**
     * 网络类型
     * 4G, 3G, 2G, WIFI等
     */
    @Column(name = "network_type", length = 20)
    private String networkType;

    /**
     * 设备运行模式
     * NORMAL: 正常模式
     * POWER_SAVE: 省电模式
     * EMERGENCY: 紧急模式
     */
    @Column(name = "device_mode", length = 20)
    private String deviceMode;

    /**
     * 设备固件版本
     */
    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    /**
     * 设备运行时长（秒）
     * 从上次重启开始的运行时间
     */
    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;

    /**
     * 内存使用率（百分比）
     */
    @Column(name = "memory_usage_percent")
    private Integer memoryUsagePercent;

    // ==================== 数据来源信息====================

    /**
     * 数据来源
     * GPS: 来自GPS定位消息
     * HEARTBEAT: 来自心跳包
     * MANUAL: 手动触发
     * SCHEDULED: 定时上报
     */
    @Column(name = "data_source", nullable = false, length = 20)
    private String dataSource;

    /**
     * 原始消息ID
     * 用于追踪MQTT消息
     */
    @Column(name = "original_message_id", length = 100)
    private String originalMessageId;

    /**
     * 本地时间字符串
     * 格式：YYYY/MM/DD HH:MM:SS
     */
    @Column(name = "local_time_string", length = 20)
    private String localTimeString;

    /**
     * 数据处理状态
     * PROCESSED: 已处理
     * PENDING: 待处理
     * ERROR: 处理错误
     */
    @Column(name = "processing_status", length = 20)
    private String processingStatus;

    /**
     * 处理错误信息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

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
        if (batteryVoltageMv == null) return null;
        return batteryVoltageMv / 1000.0;
    }

    /**
     * 获取电池电量百分比（估算）
     * 基于电压估算：4.2V=100%, 3.6V=0%
     */
    public Integer getEstimatedBatteryLevel() {
        if (batteryVoltageMv == null) return null;

        double voltage = getBatteryVoltageVolts();
        if (voltage >= 4.2) return 100;
        if (voltage <= 3.6) return 0;

        // 线性估算
        return (int) ((voltage - 3.6) / 0.6 * 100);
    }

    /**
     * 检查电池是否低电量
     * 低于3.7V认为是低电量
     */
    public boolean isLowBattery() {
        Double voltage = getBatteryVoltageVolts();
        return voltage != null && voltage < 3.7;
    }

    /**
     * 检查CPM值是否异常
     * 超过阈值认为是异常（示例阈值：100）
     */
    public boolean isAbnormalCpm() {
        return cpmValue != null && cpmValue > 100;
    }

    /**
     * 检查信号是否良好
     * 信号强度 >= 3 认为是良好
     */
    public boolean hasGoodSignal() {
        return signalQuality != null && signalQuality >= 3;
    }

    /**
     * 创建设备状态记录的工厂方法（来自GPS数据）
     */
    public static DeviceStatusRecord createFromGpsData(Device device, Integer cpmValue,
                                                      Integer batteryVoltageMv, String localTimeString) {
        return DeviceStatusRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .cpmValue(cpmValue)
                .batteryVoltageMv(batteryVoltageMv)
                .dataSource("GPS")
                .processingStatus("PROCESSED")
                .localTimeString(localTimeString)
                .build();
    }

    /**
     * 创建设备状态记录的工厂方法（来自心跳数据）
     */
    public static DeviceStatusRecord createFromHeartbeat(Device device, Integer batteryVoltageMv,
                                                        Integer signalQuality, Integer memoryUsagePercent,
                                                        Long uptimeSeconds) {
        return DeviceStatusRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .batteryVoltageMv(batteryVoltageMv)
                .signalQuality(signalQuality)
                .memoryUsagePercent(memoryUsagePercent)
                .uptimeSeconds(uptimeSeconds)
                .dataSource("HEARTBEAT")
                .processingStatus("PROCESSED")
                .build();
    }

    /**
     * 创建设备状态记录的工厂方法（手动上报）
     */
    public static DeviceStatusRecord createFromManual(Device device, Integer cpmValue,
                                                     Integer batteryVoltageMv, Integer signalQuality,
                                                     Double deviceTemperature) {
        return DeviceStatusRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .cpmValue(cpmValue)
                .batteryVoltageMv(batteryVoltageMv)
                .signalQuality(signalQuality)
                .deviceTemperature(deviceTemperature)
                .dataSource("MANUAL")
                .processingStatus("PROCESSED")
                .build();
    }

    /**
     * 创建错误状态记录
     */
    public static DeviceStatusRecord createErrorRecord(Device device, String errorMessage, String dataSource) {
        return DeviceStatusRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .dataSource(dataSource)
                .processingStatus("ERROR")
                .errorMessage(errorMessage)
                .build();
    }
}