package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.RadiationDeviceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 辐射监测仪数据响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiationDeviceDataResponse {

    private Long id;
    private String deviceCode;
    private String rawData;
    private Integer src;
    private Integer msgtype;
    private Double CPM;
    private Double Batvolt;
    private String time;
    private Integer trigger;
    private Integer multi;
    private Integer way;
    private String gpsType;        // BDS 或 LBS
    private String gpsLongitude;
    private String gpsLatitude;
    private String gpsUtc;
    private LocalDateTime recordTime;

    public static RadiationDeviceDataResponse fromRadiationDeviceData(RadiationDeviceData data) {
        return RadiationDeviceDataResponse.builder()
                .id(data.getId())
                .deviceCode(data.getDeviceCode())
                .rawData(data.getRawData())
                .src(data.getSrc())
                .msgtype(data.getMsgtype())
                .CPM(data.getCpm())
                .Batvolt(data.getBatvolt())
                .time(data.getTime())
                .trigger(data.getDataTrigger())
                .multi(data.getMulti())
                .way(data.getWay())
                .gpsType(data.getGpsType())
                .gpsLongitude(data.getGpsLongitude())
                .gpsLatitude(data.getGpsLatitude())
                .gpsUtc(data.getGpsUtc())
                .recordTime(data.getRecordTime())
                .build();
    }
}