package com.cdutetc.ems.dto.event;

import com.cdutetc.ems.entity.RadiationDeviceData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

/**
 * 设备数据事件
 * 用于SSE实时推送设备数据到前端，以及触发数据上报
 * 修复：继承 ApplicationEvent 以支持 @TransactionalEventListener
 */
@Getter
public class DeviceDataEvent extends ApplicationEvent {
    private final String eventType;      // "radiation-data" | "environment-data" | "alert"
    private final String deviceCode;
    private final String deviceType;
    private final LocalDateTime eventTime;  // 重命名避免与 ApplicationEvent.getTimestamp() 冲突

    /**
     * 获取事件数据（与ApplicationEvent兼容）
     */
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) getSource();
    }

    public DeviceDataEvent(String eventType, String deviceCode, String deviceType, Object data) {
        super(data);  // ApplicationEvent 需要源对象
        this.eventType = eventType;
        this.deviceCode = deviceCode;
        this.deviceType = deviceType;
        this.eventTime = LocalDateTime.now();
    }

    /**
     * 获取辐射设备数据（仅当数据类型为 radiation-data 时有效）
     */
    public RadiationDeviceData getRadiationDeviceData() {
        Object data = getSource();  // 从 ApplicationEvent 获取数据
        if ("radiation-data".equals(eventType) && data instanceof RadiationDeviceData) {
            return (RadiationDeviceData) data;
        }
        return null;
    }
}
