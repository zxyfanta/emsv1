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
 * GPS轨迹记录实体
 * 存储设备的GPS定位轨迹信息
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Entity
@Table(name = "gps_records", indexes = {
    @Index(name = "idx_device_record_time", columnList = "device_id, record_time"),
    @Index(name = "idx_record_time", columnList = "record_time"),
    @Index(name = "idx_primary_location", columnList = "primary_longitude, primary_latitude")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsRecord {

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

    // ==================== BDS信息（北斗定位系统）====================

    /**
     * BDS经度（度分格式原始数据）
     * 例如：10343.4623 表示 103度43.4623分
     */
    @Column(name = "bds_longitude_dm")
    private Double bdsLongitudeDm;

    /**
     * BDS纬度（度分格式原始数据）
     * 例如：2936.4009 表示 29度36.4009分
     */
    @Column(name = "bds_latitude_dm")
    private Double bdsLatitudeDm;

    /**
     * BDS UTC时间
     * 格式：HHMMSS.SS，例如：150227.58
     */
    @Column(name = "bds_utc_time", length = 10)
    private String bdsUtcTime;

    /**
     * BDS经度（十进制格式，便于查询）
     */
    @Column(name = "bds_longitude_decimal")
    private Double bdsLongitudeDecimal;

    /**
     * BDS纬度（十进制格式，便于查询）
     */
    @Column(name = "bds_latitude_decimal")
    private Double bdsLatitudeDecimal;

    /**
     * BDS定位是否有效
     */
    @Column(name = "bds_useful")
    private Boolean bdsUseful;

    // ==================== LBS信息（基站定位系统）====================

    /**
     * LBS经度（十进制格式）
     */
    @Column(name = "lbs_longitude")
    private Double lbsLongitude;

    /**
     * LBS纬度（十进制格式）
     */
    @Column(name = "lbs_latitude")
    private Double lbsLatitude;

    /**
     * LBS定位是否有效
     */
    @Column(name = "lbs_useful")
    private Boolean lbsUseful;

    // ==================== 主位置信息（定位优先级：BDS > LBS）====================

    /**
     * 主要经度（优先使用BDS，不可用时使用LBS）
     */
    @Column(name = "primary_longitude", nullable = false)
    private Double primaryLongitude;

    /**
     * 主要纬度（优先使用BDS，不可用时使用LBS）
     */
    @Column(name = "primary_latitude", nullable = false)
    private Double primaryLatitude;

    /**
     * 主要定位类型
     * BDS：北斗定位
     * LBS：基站定位
     */
    @Column(name = "primary_type", nullable = false, length = 10)
    private String primaryType;

    // ==================== 其他原始数据字段====================

    /**
     * 触发类型
     * 定位触发的原因类型编码
     */
    @Column(name = "trigger_type")
    private Integer triggerType;

    /**
     * 传输方式
     * 数据传输路径标识
     */
    @Column(name = "transmission_way")
    private Integer transmissionWay;

    /**
     * 多重标识
     * 数据多重处理标识
     */
    @Column(name = "multi_flag")
    private Integer multiFlag;

    /**
     * 消息类型
     */
    @Column(name = "message_type")
    private Integer messageType;

    /**
     * 数据源标识
     */
    @Column(name = "source_flag")
    private Integer sourceFlag;

    /**
     * 本地时间字符串
     * 格式：YYYY/MM/DD HH:MM:SS
     */
    @Column(name = "local_time_string", length = 20)
    private String localTimeString;

    /**
     * 数据创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== 业务方法====================

    /**
     * 检查是否有有效的位置信息
     */
    public boolean hasValidLocation() {
        return (bdsUseful != null && bdsUseful) || (lbsUseful != null && lbsUseful);
    }

    /**
     * 获取定位精度等级
     * BDS定位精度高于LBS
     */
    public String getAccuracyLevel() {
        if ("BDS".equals(primaryType)) {
            return "HIGH";
        } else if ("LBS".equals(primaryType)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 计算与另一个GPS点的距离（米）
     * 使用Haversine公式计算球面距离
     */
    public Double distanceTo(GpsRecord other) {
        if (other == null) return null;

        double lat1 = Math.toRadians(this.primaryLatitude);
        double lon1 = Math.toRadians(this.primaryLongitude);
        double lat2 = Math.toRadians(other.primaryLatitude);
        double lon2 = Math.toRadians(other.primaryLongitude);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return 6371000 * c; // 地球半径6371公里
    }

    /**
     * 创建GPS记录的工厂方法
     */
    public static GpsRecord createFromMqttData(Device device, String deviceId,
                                               Double bdsLng, Double bdsLat, String utcTime, Boolean bdsUseful,
                                               Double lbsLng, Double lbsLat, Boolean lbsUseful,
                                               Integer triggerType, Integer transmissionWay,
                                               Integer multiFlag, Integer messageType, Integer sourceFlag,
                                               String localTimeString) {
        GpsRecord.GpsRecordBuilder builder = GpsRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .bdsLongitudeDm(bdsLng)
                .bdsLatitudeDm(bdsLat)
                .bdsUtcTime(utcTime)
                .bdsUseful(bdsUseful != null && bdsUseful)
                .lbsLongitude(lbsLng)
                .lbsLatitude(lbsLat)
                .lbsUseful(lbsUseful != null && lbsUseful)
                .triggerType(triggerType)
                .transmissionWay(transmissionWay)
                .multiFlag(multiFlag)
                .messageType(messageType)
                .sourceFlag(sourceFlag)
                .localTimeString(localTimeString);

        // 处理BDS坐标转换（度分转十进制）
        if (bdsLng != null && bdsLat != null && bdsUseful != null && bdsUseful) {
            Double bdsLngDecimal = convertBdsToDecimal(bdsLng);
            Double bdsLatDecimal = convertBdsToDecimal(bdsLat);
            builder.bdsLongitudeDecimal(bdsLngDecimal)
                   .bdsLatitudeDecimal(bdsLatDecimal)
                   .primaryLongitude(bdsLngDecimal)
                   .primaryLatitude(bdsLatDecimal)
                   .primaryType("BDS");
        }
        // 如果BDS不可用，使用LBS
        else if (lbsLng != null && lbsLat != null && lbsUseful != null && lbsUseful) {
            builder.primaryLongitude(lbsLng)
                   .primaryLatitude(lbsLat)
                   .primaryType("LBS");
        }
        // 如果都不可用，使用任意一个坐标
        else if (bdsLng != null && bdsLat != null) {
            Double bdsLngDecimal = convertBdsToDecimal(bdsLng);
            Double bdsLatDecimal = convertBdsToDecimal(bdsLat);
            builder.bdsLongitudeDecimal(bdsLngDecimal)
                   .bdsLatitudeDecimal(bdsLatDecimal)
                   .primaryLongitude(bdsLngDecimal)
                   .primaryLatitude(bdsLatDecimal)
                   .primaryType("BDS");
        } else if (lbsLng != null && lbsLat != null) {
            builder.primaryLongitude(lbsLng)
                   .primaryLatitude(lbsLat)
                   .primaryType("LBS");
        }

        return builder.build();
    }

    /**
     * BDS坐标转换：度分格式转十进制
     * 例如：10343.4623 -> 103.724371
     */
    private static Double convertBdsToDecimal(Double bdsCoordinate) {
        if (bdsCoordinate == null) return null;

        Double degrees = Math.floor(bdsCoordinate / 100);
        Double minutes = bdsCoordinate % 100;
        return degrees + minutes / 60.0;
    }
}