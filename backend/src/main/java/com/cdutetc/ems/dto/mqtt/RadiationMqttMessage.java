package com.cdutetc.ems.dto.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 辐射设备MQTT消息DTO
 * 对应原始JSON格式: {"src":1,"msgtype":1,"CPM":123,"Batvolt":3989,"time":"2025/01/15 14:30:45","trigger":1,"multi":1,"way":1,"BDS":{...},"LBS":{...}}
 *
 * @author EMS Team
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadiationMqttMessage {

    /**
     * 数据源标识
     */
    @JsonProperty("src")
    private Integer src;

    /**
     * 消息类型
     */
    @JsonProperty("msgtype")
    private Integer msgtype;

    /**
     * CPM辐射值（每分钟计数）
     */
    @JsonProperty("CPM")
    private Double CPM;

    /**
     * 电池电压（毫伏）
     */
    @JsonProperty("Batvolt")
    private Double Batvolt;

    /**
     * 时间字符串
     */
    @JsonProperty("time")
    private String time;

    /**
     * 触发标志
     */
    @JsonProperty("trigger")
    private Integer trigger;

    /**
     * 倍数标志
     */
    @JsonProperty("multi")
    private Integer multi;

    /**
     * 通道标志
     */
    @JsonProperty("way")
    private Integer way;

    /**
     * BDS北斗定位数据
     */
    @JsonProperty("BDS")
    private GpsData BDS;

    /**
     * LBS基站定位数据
     */
    @JsonProperty("LBS")
    private GpsData LBS;

    /**
     * GPS定位数据（通用结构）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GpsData {
        /**
         * 经度（度分格式或十进制格式）
         */
        @JsonProperty("longitude")
        private String longitude;

        /**
         * 纬度（度分格式或十进制格式）
         */
        @JsonProperty("latitude")
        private String latitude;

        /**
         * UTC时间（BDS专用）
         */
        @JsonProperty("UTC")
        private String UTC;

        /**
         * 定位是否有效（1=有效，0=无效）
         */
        @JsonProperty("useful")
        private Integer useful;
    }
}
