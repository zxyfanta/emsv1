package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String activationStatus;
    private String description;
    private String location;
    private String status;
    private Integer positionX;
    private Integer positionY;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDateTime productionDate;
    private LocalDateTime installDate;
    private LocalDateTime lastOnlineAt;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== 辐射设备上报专用字段 ====================
    private String nuclide;
    private String inspectionMachineNumber;
    private String sourceNumber;
    private String sourceType;
    private String originalActivity;
    private String currentActivity;
    private LocalDate sourceProductionDate;

    // ==================== 数据上报配置字段 ====================
    private Boolean dataReportEnabled;
    private String reportProtocol;
    private LocalDateTime lastReportTime;
    private String lastReportStatus;
    private String lastReportError;
    private Integer totalReportCount;
    private Integer successReportCount;

    public static DeviceResponse fromDevice(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType().name())
                .activationStatus(device.getActivationStatus() != null ? device.getActivationStatus().name() : null)
                .description(device.getDescription())
                .location(device.getLocation())
                .status(device.getStatus().name())
                .positionX(device.getPositionX())
                .positionY(device.getPositionY())
                .serialNumber(device.getSerialNumber())
                .manufacturer(device.getManufacturer())
                .model(device.getModel())
                .productionDate(device.getProductionDate())
                .installDate(device.getInstallDate())
                .lastOnlineAt(device.getLastOnlineAt())
                .companyName(device.getCompany() != null ? device.getCompany().getCompanyName() : null)
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                // 辐射设备上报专用字段
                .nuclide(device.getNuclide())
                .inspectionMachineNumber(device.getInspectionMachineNumber())
                .sourceNumber(device.getSourceNumber())
                .sourceType(device.getSourceType())
                .originalActivity(device.getOriginalActivity())
                .currentActivity(device.getCurrentActivity())
                .sourceProductionDate(device.getSourceProductionDate())
                // 数据上报配置字段
                .dataReportEnabled(device.getDataReportEnabled())
                .reportProtocol(device.getReportProtocol())
                .lastReportTime(device.getLastReportTime())
                .lastReportStatus(device.getLastReportStatus())
                .lastReportError(device.getLastReportError())
                .totalReportCount(device.getTotalReportCount())
                .successReportCount(device.getSuccessReportCount())
                .build();
    }
}