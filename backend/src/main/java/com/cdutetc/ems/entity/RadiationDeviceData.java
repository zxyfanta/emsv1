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

    @Column(name = "trigger")
    private Integer trigger;

    @Column(name = "multi")
    private Integer multi;

    @Column(name = "way")
    private Integer way;

    // BDS定位信息
    @Column(name = "BDS_longitude", length = 50)
    private String bdsLongitude;

    @Column(name = "BDS_latitude", length = 50)
    private String bdsLatitude;

    @Column(name = "BDS_UTC", length = 50)
    private String bdsUtc;

    @Column(name = "BDS_useful")
    private Integer bdsUseful;

    // LBS定位信息
    @Column(name = "LBS_longitude", length = 50)
    private String lbsLongitude;

    @Column(name = "LBS_latitude", length = 50)
    private String lbsLatitude;

    @Column(name = "LBS_useful")
    private Integer lbsUseful;

    @CreationTimestamp
    @Column(name = "record_time", nullable = false, updatable = false)
    private LocalDateTime recordTime;
}