package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.DeviceActivationCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 已激活设备响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivatedDeviceResponse {

    private Long id;
    private String deviceCode;
    private String deviceType;
    private String serialNumber;
    private String company;
    private LocalDateTime activatedAt;
    private String activatedBy;

    public static ActivatedDeviceResponse fromEntity(Device device, DeviceActivationCode code) {
        return ActivatedDeviceResponse.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType().name())
                .serialNumber(device.getSerialNumber())
                .company(device.getCompany() != null ? device.getCompany().getCompanyName() : "未知")
                .activatedAt(code.getUsedAt())
                .activatedBy(code.getUsedByUsername())
                .build();
    }
}
