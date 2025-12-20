package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 辐射设备数据接收请求DTO
 */
@Data
public class RadiationDataReceiveRequest {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @NotBlank(message = "原始数据不能为空")
    private String rawData;

    @NotNull(message = "设备源不能为空")
    private Integer src;

    @NotNull(message = "消息类型不能为空")
    private Integer msgtype;

    @NotNull(message = "CPM值不能为空")
    @DecimalMin(value = "0.0", message = "CPM值必须大于等于0")
    private Double cpm;

    @NotNull(message = "电池电压不能为空")
    @DecimalMin(value = "0.0", message = "电池电压必须大于等于0")
    private Double batvolt;

    @NotBlank(message = "时间不能为空")
    private String time;

    @NotNull(message = "触发值不能为空")
    private Integer trigger;

    @NotNull(message = "倍数不能为空")
    private Integer multi;

    @NotNull(message = "方式不能为空")
    private Integer way;

    // BDS位置信息
    private String bdsLongitude;
    private String bdsLatitude;
    private String bdsUtc;

    // LBS位置信息
    private String lbsLongitude;
    private String lbsLatitude;
    private Integer lbsUseful;
}