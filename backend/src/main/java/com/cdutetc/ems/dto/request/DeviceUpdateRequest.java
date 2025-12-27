package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

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

    // ==================== 辐射设备上报专用字段 ====================
    // 注意：这些字段仅对辐射设备有效

    @Size(max = 50, message = "核素长度不能超过50个字符")
    private String nuclide;

    @Size(max = 50, message = "探伤机编号长度不能超过50个字符")
    private String inspectionMachineNumber;

    @Size(max = 12, message = "放射源编号长度不能超过12个字符")
    private String sourceNumber;

    @Size(max = 2, message = "放射源类型长度不能超过2个字符")
    private String sourceType;

    @Size(max = 20, message = "原始活度长度不能超过20个字符")
    private String originalActivity;

    @Size(max = 20, message = "当前活度长度不能超过20个字符")
    private String currentActivity;

    private LocalDate sourceProductionDate;

    // ==================== 数据上报配置字段 ====================

    /**
     * 是否启用数据上报
     */
    private Boolean dataReportEnabled;

    /**
     * 上报协议类型（SICHUAN/SHANDONG）
     */
    @Size(max = 20, message = "上报协议类型长度不能超过20个字符")
    private String reportProtocol;

    /**
     * GPS优先级（BDS/LBS/BDS_THEN_LBS）
     */
    @Size(max = 20, message = "GPS优先级长度不能超过20个字符")
    private String gpsPriority;
}