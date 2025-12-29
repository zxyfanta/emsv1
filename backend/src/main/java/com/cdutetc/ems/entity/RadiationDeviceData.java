package com.cdutetc.ems.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 辐射监测仪数据实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ems_radiation_device_data")
public class RadiationDeviceData extends BaseEntity {

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

    @Column(name = "msgtype")
    private Integer msgtype;

    @Column(name = "CPM")
    private Double cpm;

    @Column(name = "Batvolt")
    private Double batvolt;

    @Column(name = "time", length = 20)
    private String time;

    @Column(name = "data_trigger")
    private Integer dataTrigger;

    @Column(name = "multi")
    private Integer multi;

    @Column(name = "way")
    private Integer way;

    // 统一的GPS字段（存储根据useful选择后的GPS数据）
    @Column(name = "gps_longitude", length = 50)
    private String gpsLongitude;

    @Column(name = "gps_latitude", length = 50)
    private String gpsLatitude;

    @Column(name = "gps_type", length = 20)
    private String gpsType;  // BDS 或 LBS

    @Column(name = "gps_utc", length = 50)
    private String gpsUtc;   // 仅BDS有值，LBS为null

    @CreationTimestamp
    @Column(name = "record_time", nullable = false, updatable = false)
    private LocalDateTime recordTime;
}