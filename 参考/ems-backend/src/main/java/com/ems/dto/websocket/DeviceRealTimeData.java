package com.ems.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备实时数据DTO
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRealTimeData {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 数据时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 传感器数据
     */
    private SensorData sensorData;

    /**
     * GPS数据
     */
    private GpsData gpsData;

    /**
     * 在线状态
     */
    private Boolean online;

    /**
     * 设备工作模式
     */
    private Integer workMode;

    /**
     * 触发方式
     */
    private Integer triggerMode;

    /**
     * 传感器数据内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorData {
        /**
         * CPM辐射计数
         */
        private Double CPM;

        /**
         * 电池电压
         */
        private Integer Batvolt;

        /**
         * 其他传感器数据可以在这里扩展
         */
        private Double temperature;
        private Double humidity;
    }

    /**
     * GPS数据内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsData {
        /**
         * 经度
         */
        private Double longitude;

        /**
         * 纬度
         */
        private Double latitude;

        /**
         * 定位精度
         */
        private Double accuracy;

        /**
         * 定位方式：BDS/LBS
         */
        private String locationType;

        /**
         * 定位时间戳
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime locationTime;
    }
}