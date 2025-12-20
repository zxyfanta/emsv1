package com.cdutetc.ems.dto.request;

import com.cdutetc.ems.entity.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备创建请求DTO
 */
@Data
public class DeviceCreateRequest {

    @NotBlank(message = "设备编码不能为空")
    @Size(max = 50, message = "设备编码长度不能超过50个字符")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    @NotNull(message = "设备类型不能为空")
    private String deviceType;

    @Size(max = 500, message = "设备描述长度不能超过500个字符")
    private String description;

    @Size(max = 255, message = "设备位置长度不能超过255个字符")
    private String location;
}