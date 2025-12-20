package com.cdutetc.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备数据接收响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDataReceiveResponse {

    /**
     * 接收是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 保存的数据ID
     */
    private Long deviceId;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 接收时间
     */
    private LocalDateTime receiveTime;
}