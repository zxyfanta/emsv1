package com.cdutetc.ems.dto.mqtt;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * MQTT设备数据消息DTO
 * 用于统一处理来自MQTT的设备数据消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MqttDeviceDataMessage {

    /**
     * 设备编码
     */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /**
     * 设备类型
     */
    @NotNull(message = "设备类型不能为空")
    private String deviceType; // radiation 或 environment

    /**
     * 源标识
     */
    @NotNull(message = "源标识不能为空")
    private Integer src;

    /**
     * CPM值（辐射测量值）
     */
    private Double cpm;

    /**
     * 电池电压（辐射设备）
     */
    private Double batvolt;

    /**
     * 消息类型（辐射设备）
     */
    private Integer msgtype;

    /**
     * 触发类型（辐射设备）
     */
    private Integer trigger;

    /**
     * 倍数（辐射设备）
     */
    private Integer multi;

    /**
     * 方式（辐射设备）
     */
    private Integer way;

    /**
     * 温度（环境设备）
     */
    private Double temperature;

    /**
     * 湿度（环境设备）
     */
    private Double wetness;

    /**
     * 风速（环境设备）
     */
    private Double windspeed;

    /**
     * 总量（环境设备）
     */
    private Double total;

    /**
     * 电池电量（环境设备）
     */
    private Double battery;

    /**
     * 时间戳（字符串格式）
     */
    private String time;

    /**
     * BDS经度
     */
    private String bdsLongitude;

    /**
     * BDS纬度
     */
    private String bdsLatitude;

    /**
     * BDS时间
     */
    private String bdsUtc;

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
    private Integer lbsUseful;

    /**
     * 原始数据
     */
    private String rawData;

    /**
     * 消息接收时间
     */
    @Builder.Default
    private LocalDateTime receiveTime = LocalDateTime.now();

    // 位置信息内部类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        @JsonProperty("longitude")
        private String longitude;

        @JsonProperty("latitude")
        private String latitude;

        @JsonProperty("useful")
        private Integer useful;

        @JsonProperty("UTC")
        private String utc;
    }

    /**
     * 验证消息是否为有效格式
     */
    public boolean isValid() {
        return deviceCode != null && !deviceCode.isEmpty()
                && deviceType != null
                && src != null;
    }

    /**
     * 判断是否为辐射设备数据
     */
    public boolean isRadiationDevice() {
        return "radiation".equalsIgnoreCase(deviceType);
    }

    /**
     * 判断是否为环境设备数据
     */
    public boolean isEnvironmentDevice() {
        return "environment".equalsIgnoreCase(deviceType);
    }

    /**
     * 转换为辐射设备数据请求DTO
     */
    public com.cdutetc.ems.dto.request.RadiationDataReceiveRequest toRadiationDataRequest() {
        if (!isRadiationDevice()) {
            throw new IllegalArgumentException("不是辐射设备数据");
        }

        com.cdutetc.ems.dto.request.RadiationDataReceiveRequest request =
                new com.cdutetc.ems.dto.request.RadiationDataReceiveRequest();
        request.setDeviceCode(deviceCode);
        request.setRawData(rawData != null ? rawData : "");
        request.setSrc(src);
        request.setMsgtype(msgtype != null ? msgtype : 1);
        request.setCpm(cpm);
        request.setBatvolt(batvolt);
        request.setTime(time != null ? time : LocalDateTime.now().toString());
        request.setTrigger(trigger != null ? trigger : 1);
        request.setMulti(multi != null ? multi : 1);
        request.setWay(way != null ? way : 1);
        request.setBdsLongitude(bdsLongitude);
        request.setBdsLatitude(bdsLatitude);
        request.setBdsUtc(bdsUtc);
        request.setLbsLongitude(lbsLongitude);
        request.setLbsLatitude(lbsLatitude);
        request.setLbsUseful(lbsUseful);

        return request;
    }

    /**
     * 转换为环境设备数据请求DTO
     */
    public com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest toEnvironmentDataRequest() {
        if (!isEnvironmentDevice()) {
            throw new IllegalArgumentException("不是环境设备数据");
        }

        com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest request =
                new com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest();
        request.setDeviceCode(deviceCode);
        request.setRawData(rawData != null ? rawData : "");
        request.setSrc(src);
        request.setCpm(cpm);
        request.setTemperature(temperature);
        request.setWetness(wetness);
        request.setWindspeed(windspeed);
        request.setTotal(total);
        request.setBattery(battery);

        return request;
    }

    /**
     * 从JSON字符串创建MQTT消息对象
     */
    public static MqttDeviceDataMessage fromJson(String json, String deviceCode, String deviceType) {
        // 这里可以添加JSON解析逻辑
        // 暂时返回基础对象，实际使用时需要根据JSON格式进行解析
        return MqttDeviceDataMessage.builder()
                .deviceCode(deviceCode)
                .deviceType(deviceType)
                .rawData(json)
                .receiveTime(LocalDateTime.now())
                .build();
    }
}