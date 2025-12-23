package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.Alert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警响应DTO
 */
@Data
public class AlertResponse {
    private Long id;
    private String alertType;
    private String alertTypeDescription;
    private String severity;
    private String severityDescription;
    private String deviceCode;
    private Long deviceId;
    private String deviceName;
    private String message;
    private Object data;  // 告警详细数据（解析后的JSON对象）
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从实体转换为响应DTO
     */
    public static AlertResponse fromAlert(Alert alert) {
        AlertResponse response = new AlertResponse();
        response.setId(alert.getId());
        response.setAlertType(alert.getAlertType());
        response.setSeverity(alert.getSeverity());
        response.setDeviceCode(alert.getDeviceCode());
        response.setMessage(alert.getMessage());
        response.setResolved(alert.getResolved());
        response.setResolvedAt(alert.getResolvedAt());
        response.setCreatedAt(alert.getCreatedAt());

        // 设置告警类型描述
        try {
            response.setAlertTypeDescription(
                com.cdutetc.ems.entity.enums.AlertType.fromCode(alert.getAlertType()).getDescription()
            );
        } catch (Exception e) {
            response.setAlertTypeDescription(alert.getAlertType());
        }

        // 设置严重程度描述
        try {
            response.setSeverityDescription(
                com.cdutetc.ems.entity.enums.AlertSeverity.fromCode(alert.getSeverity()).getDescription()
            );
        } catch (Exception e) {
            response.setSeverityDescription(alert.getSeverity());
        }

        // 设置设备信息
        if (alert.getDevice() != null) {
            response.setDeviceId(alert.getDevice().getId());
            response.setDeviceName(alert.getDevice().getDeviceName());
        }

        // 解析JSON数据
        if (alert.getData() != null) {
            try {
                response.setData(objectMapper.readValue(alert.getData(), Object.class));
            } catch (JsonProcessingException e) {
                response.setData(alert.getData());  // 解析失败返回原始字符串
            }
        }

        return response;
    }
}
