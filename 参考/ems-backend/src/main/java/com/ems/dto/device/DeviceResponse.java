package com.ems.dto.device;

import com.ems.entity.device.Device;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备响应DTO
 *
 * @author EMS Team
 */
@Data
public class DeviceResponse {

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
     * 设备唯一编号
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
     * 最后在线时间
     */
    private LocalDateTime lastOnlineAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从Device实体转换为响应DTO
     */
    public static DeviceResponse fromEntity(Device device) {
        DeviceResponse response = new DeviceResponse();
        response.setId(device.getId());
        response.setDeviceId(device.getDeviceId());
        response.setDeviceName(device.getDeviceName());
        response.setStatus(device.getStatus().name());
        response.setStatusDescription(device.getStatus().getDescription());
        response.setLastOnlineAt(device.getLastOnlineAt());
        response.setCreatedAt(device.getCreatedAt());
        
        // 设置企业信息
        if (device.getEnterprise() != null) {
            response.setEnterpriseId(device.getEnterprise().getId());
            response.setEnterpriseName(device.getEnterprise().getName());
        }
        
        return response;
    }
}