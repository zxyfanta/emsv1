package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.DeviceActivationCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待激活设备响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDeviceResponse {

    private Long id;
    private String deviceCode;
    private String deviceType;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDateTime productionDate;
    private String activationCode;
    private LocalDateTime activationCodeExpiresAt;

    public static PendingDeviceResponse fromEntity(Device device, DeviceActivationCode code) {
        return PendingDeviceResponse.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType().name())
                .serialNumber(device.getSerialNumber())
                .manufacturer(device.getManufacturer())
                .model(device.getModel())
                .productionDate(device.getProductionDate())
                .activationCode(code.getCode())
                .activationCodeExpiresAt(code.getExpiresAt())
                .build();
    }
}
