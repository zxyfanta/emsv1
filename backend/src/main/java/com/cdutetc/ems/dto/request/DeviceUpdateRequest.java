package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备更新请求DTO
 */
@Data
public class DeviceUpdateRequest {

    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    @Size(max = 500, message = "设备描述长度不能超过500个字符")
    private String description;

    @Size(max = 255, message = "设备位置长度不能超过255个字符")
    private String location;
}