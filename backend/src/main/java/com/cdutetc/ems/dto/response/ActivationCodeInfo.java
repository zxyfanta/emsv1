package com.cdutetc.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 激活码信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationCodeInfo {

    private String code;
    private String deviceCode;
    private String deviceType;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDateTime productionDate;
    private LocalDateTime expiresAt;
}
