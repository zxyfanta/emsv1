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
    private String description;
    private String location;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeviceResponse fromDevice(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType().name())
                .description(device.getDescription())
                .location(device.getLocation())
                .status(device.getStatus().name())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}