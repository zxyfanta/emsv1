package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 环境设备数据接收请求DTO
 */
@Data
public class EnvironmentDataReceiveRequest {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @NotBlank(message = "原始数据不能为空")
    private String rawData;

    @NotNull(message = "设备源不能为空")
    private Integer src;

    @NotNull(message = "CPM值不能为空")
    @DecimalMin(value = "0.0", message = "CPM值必须大于等于0")
    private Double cpm;

    @NotNull(message = "温度不能为空")
    private Double temperature;

    @NotNull(message = "湿度不能为空")
    private Double wetness;

    @NotNull(message = "风速不能为空")
    private Double windspeed;

    @NotNull(message = "综合值不能为空")
    private Double total;

    @NotNull(message = "电池电压不能为空")
    @DecimalMin(value = "0.0", message = "电池电压必须大于等于0")
    private Double battery;
}