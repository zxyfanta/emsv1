package com.cdutetc.ems.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 环境监测站数据实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ems_environment_device_data")
public class EnvironmentDeviceData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "设备编码不能为空")
    @Column(name = "device_code", nullable = false)
    private String deviceCode;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    @Column(name = "src")
    private Integer src;

    @Column(name = "CPM")
    private Integer cpm;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "wetness")
    private Double wetness;

    @Column(name = "windspeed")
    private Double windspeed;

    @Column(name = "total")
    private Double total;

    @Column(name = "battery")
    private Double battery;

    @CreationTimestamp
    @Column(name = "record_time", nullable = false, updatable = false)
    private LocalDateTime recordTime;
}