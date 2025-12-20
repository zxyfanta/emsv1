package com.ems.service.mqtt;

/**
 * 设备数据处理器接口
 * 定义不同设备类型的数据处理规范
 *
 * @author EMS Team
 */
public interface DeviceDataProcessor {

    /**
     * 检查是否支持指定的设备类型
     *
     * @param deviceType 设备类型代码
     * @return 是否支持
     */
    boolean supports(String deviceType);

    /**
     * 处理MQTT消息
     *
     * @param deviceId 设备ID
     * @param topic MQTT主题
     * @param payload 消息载荷
     */
    void processMessage(String deviceId, String topic, String payload);
}