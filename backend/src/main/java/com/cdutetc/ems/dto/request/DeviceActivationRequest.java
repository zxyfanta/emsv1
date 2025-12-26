package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备激活请求DTO
 */
@Data
public class DeviceActivationRequest {

    @NotBlank(message = "激活码不能为空")
    private String activationCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    @Size(max = 500, message = "设备描述长度不能超过500个字符")
    private String description;

    @NotBlank(message = "安装位置不能为空")
    @Size(max = 255, message = "安装位置长度不能超过255个字符")
    private String location;

    @Min(value = 0, message = "X坐标不能小于0")
    @Max(value = 100, message = "X坐标不能大于100")
    private Integer positionX;

    @Min(value = 0, message = "Y坐标不能小于0")
    @Max(value = 100, message = "Y坐标不能大于100")
    private Integer positionY;

    private String clientIp;
}
