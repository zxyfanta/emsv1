package com.ems.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import com.ems.entity.device.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 环境监测站状态记录实体
 * 严格按照环境监测设备数据格式设计：{"src": 1, "CPM": 4, "temperature": 10, "wetness": 95, "windspeed": 0.2, "total": 144.1, "battery": 11.9}
 *
 * @author EMS Team
 */
@Entity
@Table(name = "environment_device_status", indexes = {
    @Index(name = "idx_env_device_time", columnList = "device_id, record_time"),
    @Index(name = "idx_env_record_time", columnList = "record_time"),
    @Index(name = "idx_env_cpm_value", columnList = "cpm_value"),
    @Index(name = "idx_env_temperature", columnList = "temperature")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentDeviceStatus {

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

    /**
     * 数据源标识
     */
    @Column(name = "src")
    private Integer src;

    /**
     * CPM辐射值
     */
    @Column(name = "cpm_value")
    private Integer cpmValue;

    /**
     * 温度（摄氏度）
     */
    @Column(name = "temperature")
    private Double temperature;

    /**
     * 湿度（百分比）
     */
    @Column(name = "wetness")
    private Double wetness;

    /**
     * 风速（米/秒）
     */
    @Column(name = "wind_speed")
    private Double windSpeed;

    /**
     * 综合环境指数
     */
    @Column(name = "total_environment_index", precision = 6, scale = 2)
    private BigDecimal totalEnvironmentIndex;

    /**
     * 电池电压（伏特）
     */
    @Column(name = "battery_voltage", precision = 5, scale = 2)
    private BigDecimal batteryVoltage;

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
        return batteryVoltage != null ? batteryVoltage.doubleValue() : null;
    }

    /**
     * 检查电池是否低电量
     * 环境监测站阈值：12.0V
     */
    public boolean isLowBattery() {
        Double voltage = getBatteryVoltageVolts();
        return voltage != null && voltage < 12.0;
    }

    /**
     * 检查辐射值是否异常
     * 环境监测站阈值：50
     */
    public boolean isHighCpm() {
        return cpmValue != null && cpmValue > 50;
    }

    /**
     * 检查温度是否异常
     * 正常范围：-10°C 到 +50°C
     */
    public boolean isAbnormalTemperature() {
        return temperature != null && (temperature < -10 || temperature > 50);
    }

    /**
     * 检查湿度是否异常
     * 正常范围：20% 到 90%
     */
    public boolean isAbnormalHumidity() {
        return wetness != null && (wetness < 20 || wetness > 90);
    }

    /**
     * 检查风速是否异常
     * 正常范围：0 到 30 m/s
     */
    public boolean isAbnormalWindSpeed() {
        return windSpeed != null && (windSpeed < 0 || windSpeed > 30);
    }

    /**
     * 检查是否有任何异常数据
     */
    public boolean hasAnyAbnormalData() {
        return isLowBattery() || isHighCpm() || isAbnormalTemperature() ||
               isAbnormalHumidity() || isAbnormalWindSpeed();
    }

    // ==================== 静态工厂方法====================

    /**
     * 从MQTT数据创建环境监测站状态记录
     */
    public static EnvironmentDeviceStatus createFromMqttData(Device device, String jsonData) {
        EnvironmentDeviceStatus status = EnvironmentDeviceStatus.builder()
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
    public static EnvironmentDeviceStatus createErrorRecord(Device device, String errorMessage) {
        return EnvironmentDeviceStatus.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .build();
    }
}