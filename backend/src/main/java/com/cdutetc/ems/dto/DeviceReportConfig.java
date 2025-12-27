package com.cdutetc.ems.dto;

import com.cdutetc.ems.entity.Device;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 设备上报配置DTO（用于Redis缓存）
 * 只包含上报所需的必要字段，减少缓存空间占用
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceReportConfig {

    // 基本信息
    private String deviceCode;
    private String deviceType;  // 使用String存储，避免序列化问题

    // 辐射设备上报专用字段
    private String nuclide;
    private String inspectionMachineNumber;
    private String sourceNumber;
    private String sourceType;
    private String originalActivity;
    private String currentActivity;
    private String sourceProductionDate;

    // 上报配置
    private Boolean dataReportEnabled;
    private String reportProtocol;
    private String gpsPriority;

    /**
     * 从Device实体转换为DTO
     */
    public static DeviceReportConfig fromDevice(Device device) {
        DeviceReportConfig config = new DeviceReportConfig();
        config.setDeviceCode(device.getDeviceCode());
        config.setDeviceType(device.getDeviceType().name());  // 存储枚举名称

        // 辐射设备专用字段
        config.setNuclide(device.getNuclide());
        config.setInspectionMachineNumber(device.getInspectionMachineNumber());
        config.setSourceNumber(device.getSourceNumber());
        config.setSourceType(device.getSourceType());
        config.setOriginalActivity(device.getOriginalActivity());
        config.setCurrentActivity(device.getCurrentActivity());
        if (device.getSourceProductionDate() != null) {
            config.setSourceProductionDate(device.getSourceProductionDate().toString());
        }

        // 上报配置
        config.setDataReportEnabled(device.getDataReportEnabled());
        config.setReportProtocol(device.getReportProtocol());
        config.setGpsPriority(device.getGpsPriority());

        return config;
    }
}
