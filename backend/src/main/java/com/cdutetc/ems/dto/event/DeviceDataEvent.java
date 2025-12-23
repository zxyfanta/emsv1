package com.cdutetc.ems.dto.event;

import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 设备数据事件
 * 用于SSE实时推送设备数据到前端
 */
@Getter
public class DeviceDataEvent {
    private final String eventType;      // "radiation-data" | "environment-data" | "alert"
    private final String deviceCode;
    private final String deviceType;
    private final Object data;
    private final LocalDateTime timestamp;

    public DeviceDataEvent(String eventType, String deviceCode, String deviceType, Object data) {
        this.eventType = eventType;
        this.deviceCode = deviceCode;
        this.deviceType = deviceType;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
