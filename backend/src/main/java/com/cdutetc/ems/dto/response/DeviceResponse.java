package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
                .build();
    }
}