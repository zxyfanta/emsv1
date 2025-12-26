package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.DeviceActivationCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 激活码响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationCodeResponse {

    private Long id;
    private String code;
    private String deviceCode;
    private String deviceType;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private String status;

    public static ActivationCodeResponse fromEntity(DeviceActivationCode entity) {
        return ActivationCodeResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .deviceCode(entity.getDevice().getDeviceCode())
                .deviceType(entity.getDevice().getDeviceType().name())
                .generatedAt(entity.getGeneratedAt())
                .expiresAt(entity.getExpiresAt())
                .status(entity.getStatus().name())
                .build();
    }
}
