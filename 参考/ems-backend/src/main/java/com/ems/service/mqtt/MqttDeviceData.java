package com.ems.service.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MQTT设备数据传输对象
 * 用于存储从MQTT消息解析的设备数据
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttDeviceData {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 数据时间戳
     */
    private LocalDateTime timestamp;

    /**
     * CPM（每分钟计数）
     */
    private BigDecimal cpm;

    /**
     * 电池电压
     */
    private BigDecimal batteryVoltage;

    /**
     * BDS经度
     */
    private String bdsLongitude;

    /**
     * BDS纬度
     */
    private String bdsLatitude;

    /**
     * BDS是否有效
     */
    private Boolean bdsUseful;

    /**
     * LBS经度
     */
    private String lbsLongitude;

    /**
     * LBS纬度
     */
    private String lbsLatitude;

    /**
     * LBS是否有效
     */
    private Boolean lbsUseful;
}