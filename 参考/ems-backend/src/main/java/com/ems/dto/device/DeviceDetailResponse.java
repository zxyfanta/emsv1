package com.ems.dto.device;

import com.ems.dto.device.DeviceSpecifications;
import com.ems.dto.device.DeviceRealTimeData;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备详细信息响应
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDetailResponse {

    /**
     * 设备ID
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long enterpriseId;

    /**
     * 企业名称
     */
    private String enterpriseName;

    /**
     * 设备编号
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDescription;

    /**
     * 安装位置
     */
    private String installLocation;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备规格信息
     */
    private DeviceSpecifications specifications;

    /**
     * 最后在线时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOnlineAt;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 最后维护时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMaintenanceDate;

    /**
     * 下次维护时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceDate;

    /**
     * 设备描述
     */
    private String description;

    /**
     * 实时数据
     */
    private DeviceRealTimeData realTimeData;

    /**
     * 从Device实体创建响应（简化版本）
     */
    public static DeviceDetailResponse fromSimpleDevice(com.ems.entity.device.Device device) {
        if (device == null) {
            return null;
        }

        return DeviceDetailResponse.builder()
                .id(device.getId())
                .enterpriseId(device.getEnterpriseId())
                .enterpriseName(device.getEnterprise() != null ? device.getEnterprise().getName() : null)
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .status(device.getStatus().name())
                .statusDescription(device.getStatus().getDescription())
                .lastOnlineAt(device.getLastOnlineAt())
                .createdAt(device.getCreatedAt())
                // 其他字段设为默认值，后续可以扩展
                .installLocation("未设置")
                .deviceType("通用设备")
                .description("暂无描述")
                .build();
    }
}