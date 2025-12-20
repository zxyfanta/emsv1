package com.ems.dto.device;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备更新请求DTO
 *
 * @author EMS Team
 */
@Data
public class DeviceUpdateRequest {

    /**
     * 设备名称
     */
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    /**
     * 设备状态
     */
    private String status;
}