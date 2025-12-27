package com.cdutetc.ems.dto.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 环境监测站MQTT消息DTO
 * 对应原始JSON格式: {"src":1,"CPM":4,"temperature":10,"wetness":95,"windspeed":0.2,"total":144.1,"battery":11.9}
 *
 * @author EMS Team
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentMqttMessage {

    /**
     * 数据源标识
     */
    @JsonProperty("src")
    private Integer src;

    /**
     * CPM辐射值
     */
    @JsonProperty("CPM")
    private Double CPM;

    /**
     * 温度（摄氏度）
     */
    @JsonProperty("temperature")
    private Double temperature;

    /**
     * 湿度（百分比）
     */
    @JsonProperty("wetness")
    private Double wetness;

    /**
     * 风速（米/秒）
     */
    @JsonProperty("windspeed")
    private Double windspeed;

    /**
     * 综合环境指数
     */
    @JsonProperty("total")
    private Double total;

    /**
     * 电池电压（伏特）
     */
    @JsonProperty("battery")
    private Double battery;
}
