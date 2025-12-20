package com.ems.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备创建请求DTO
 *
 * @author EMS Team
 */
@Data
public class DeviceCreateRequest {

    /**
     * 设备唯一编号
     */
    @NotBlank(message = "设备编号不能为空")
    @Size(max = 50, message = "设备编号长度不能超过50个字符")
    private String deviceId;

    /**
     * 设备名称
     */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    /**
     * 企业ID
     */
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;
}