package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备导入项DTO
 */
@Data
public class DeviceImportItem {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    @NotBlank(message = "序列号不能为空")
    private String serialNumber;

    @NotBlank(message = "厂商不能为空")
    private String manufacturer;

    @NotBlank(message = "型号不能为空")
    private String model;

    @NotNull(message = "生产日期不能为空")
    private LocalDateTime productionDate;
}
