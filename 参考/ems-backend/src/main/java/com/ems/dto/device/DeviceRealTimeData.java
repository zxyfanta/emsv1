package com.ems.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备实时数据
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRealTimeData {

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 湿度
     */
    private Double humidity;

    /**
     * 压力
     */
    private Double pressure;

    /**
     * 功率
     */
    private Double power;

    /**
     * 信号强度
     */
    private Integer signalStrength;

    /**
     * 最后数据时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastDataTime;

    /**
     * 数据间隔（秒）
     */
    private Integer dataInterval;
}