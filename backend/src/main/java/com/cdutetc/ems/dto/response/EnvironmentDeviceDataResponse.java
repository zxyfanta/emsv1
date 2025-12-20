package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.EnvironmentDeviceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 环境监测站数据响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentDeviceDataResponse {

    private Long id;
    private String deviceCode;
    private String rawData;
    private Integer src;
    private Double CPM;
    private Double temperature;
    private Double wetness;
    private Double windspeed;
    private Double total;
    private Double battery;
    private LocalDateTime recordTime;

    public static EnvironmentDeviceDataResponse fromEnvironmentDeviceData(EnvironmentDeviceData data) {
        return EnvironmentDeviceDataResponse.builder()
                .id(data.getId())
                .deviceCode(data.getDeviceCode())
                .rawData(data.getRawData())
                .src(data.getSrc())
                .CPM(data.getCpm())
                .temperature(data.getTemperature())
                .wetness(data.getWetness())
                .windspeed(data.getWindspeed())
                .total(data.getTotal())
                .battery(data.getBattery())
                .recordTime(data.getRecordTime())
                .build();
    }
}