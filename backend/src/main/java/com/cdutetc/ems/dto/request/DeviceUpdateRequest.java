package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * 可视化大屏X坐标位置
     * 范围：0-100，相对于大屏中心模型的水平位置
     */
    @Min(value = 0, message = "X坐标不能小于0")
    @Max(value = 100, message = "X坐标不能大于100")
    private Integer positionX;

    /**
     * 可视化大屏Y坐标位置
     * 范围：0-100，相对于大屏中心模型的垂直位置
     */
    @Min(value = 0, message = "Y坐标不能小于0")
    @Max(value = 100, message = "Y坐标不能大于100")
    private Integer positionY;
}