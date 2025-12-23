package com.cdutetc.ems.mqtt;

import com.cdutetc.ems.config.MqttConfig;
import com.cdutetc.ems.dto.mqtt.MqttDeviceDataMessage;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.service.DeviceService;
import com.cdutetc.ems.service.EnvironmentDeviceDataService;
import com.cdutetc.ems.service.RadiationDeviceDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MQTT消息监听器
 * 负责处理接收到的MQTT设备数据消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageListener implements MqttCallback {

    private final DeviceService deviceService;
    private final RadiationDeviceDataService radiationDeviceDataService;
    private final EnvironmentDeviceDataService environmentDeviceDataService;
    private final MqttConfig mqttConfig;
    private final ObjectMapper objectMapper;

    @Override
    public void connectionLost(Throwable cause) {
        log.error("🔌 MQTT连接丢失: {}", cause.getMessage(), cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        try {
            String payload = new String(mqttMessage.getPayload());
            log.info("📥 收到MQTT消息 - 主题: {}, 消息: {}", topic, payload);

            // 从主题中提取设备信息
            DeviceTopicInfo topicInfo = parseTopic(topic);
            log.debug("📍 解析主题信息: {}", topicInfo);

            // 确保设备存在
            Device device = getOrCreateDevice(topicInfo.getDeviceCode(), topicInfo.getDeviceType());

            // 根据消息类型处理数据
            if ("RADIATION".equalsIgnoreCase(topicInfo.getDeviceType())) {
                handleRadiationData(device, payload);
            } else if ("ENVIRONMENT".equalsIgnoreCase(topicInfo.getDeviceType())) {
                handleEnvironmentData(device, payload);
            } else {
                log.warn("⚠️ 未知的设备类型: {}", topicInfo.getDeviceType());
            }

        } catch (Exception e) {
            log.error("❌ 处理MQTT消息失败 - 主题: {}, 错误: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        if (token != null) {
            try {
                log.debug("📤 MQTT消息发送完成: {}", token.getMessage());
            } catch (Exception e) {
                log.debug("📤 MQTT消息发送完成记录出错: {}", e.getMessage());
            }
        }
    }

    /**
     * 解析MQTT主题，提取设备信息
     */
    private DeviceTopicInfo parseTopic(String topic) {
        try {
            // 主题格式: ems/device/{deviceCode}/data/{deviceType}
            String[] topicParts = topic.split("/");

            if (topicParts.length < 5) {
                throw new IllegalArgumentException("无效的MQTT主题格式: " + topic);
            }

            String deviceCode = topicParts[2]; // ems/device/{deviceCode}/data/{deviceType}
            String deviceType = topicParts[4];    // ems/device/{deviceCode}/data/{deviceType}

            return DeviceTopicInfo.builder()
                    .deviceCode(deviceCode)
                    .deviceType(deviceType)
                    .originalTopic(topic)
                    .build();
        } catch (Exception e) {
            log.error("❌ 解析MQTT主题失败: {}, 错误: {}", topic, e.getMessage());
            throw new RuntimeException("主题解析失败", e);
        }
    }

    /**
     * 获取或创建设备
     */
    private Device getOrCreateDevice(String deviceCode, String deviceTypeStr) {
        try {
            // 查找现有设备
            Device device = deviceService.findByDeviceCode(deviceCode);

            if (device == null) {
                log.info("🔧 设备不存在，自动注册: {}", deviceCode);

                // 根据主题推断设备类型
                DeviceType deviceType = "RADIATION".equalsIgnoreCase(deviceTypeStr)
                    ? DeviceType.RADIATION_MONITOR
                    : DeviceType.ENVIRONMENT_STATION;

                // 创建新设备
                device = createAutoRegisteredDevice(deviceCode, deviceType);

                log.info("✅ 设备自动注册成功: {} ({})", deviceCode, deviceType);
            }

            // 更新设备最后在线时间
            device.setLastOnlineAt(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());

            return device;

        } catch (Exception e) {
            log.error("❌ 获取或创建设备失败: {}", deviceCode, e);
            throw new RuntimeException("设备处理失败", e);
        }
    }

    /**
     * 创建自动注册的设备
     */
    private Device createAutoRegisteredDevice(String deviceCode, DeviceType deviceType) {
        try {
            Device device = new Device();
            device.setDeviceCode(deviceCode);
            device.setDeviceName("自动注册设备-" + deviceCode);
            device.setDeviceType(deviceType);
            device.setManufacturer("未知");
            device.setModel("未知");
            device.setSerialNumber("AUTO-" + deviceCode);
            device.setDescription("通过MQTT自动注册的设备");
            device.setLocation("未知");
            device.setStatus(DeviceStatus.OFFLINE); // 初始状态为离线，收到数据后会更新
            device.setCreatedAt(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());
            device.setInstallDate(LocalDateTime.now());

            // 分配到默认公司
            return deviceService.createDevice(device, mqttConfig.getDefaultCompanyId());

        } catch (Exception e) {
            log.error("❌ 创建自动注册设备失败: {}", deviceCode, e);
            throw new RuntimeException("设备自动注册失败", e);
        }
    }

    /**
     * 处理辐射设备数据
     */
    private void handleRadiationData(Device device, String payload) {
        try {
            log.debug("🔬 处理辐射设备数据: {}", device.getDeviceCode());

            // 创建辐射设备数据记录
            com.cdutetc.ems.entity.RadiationDeviceData data = new com.cdutetc.ems.entity.RadiationDeviceData();
            data.setDeviceCode(device.getDeviceCode());
            data.setRawData(payload);
            data.setRecordTime(LocalDateTime.now());

            // 尝试解析JSON数据，如果失败则保存原始数据
            try {
                // 这里可以添加JSON解析逻辑，暂时保存原始数据
                // 实际项目中需要根据Node-RED发送的具体JSON格式进行解析
                data.setSrc(1); // 默认值
                data.setMsgtype(1); // 默认值
                // 解析CPM、Batvolt等字段...
            } catch (Exception e) {
                log.warn("⚠️ 解析辐射设备数据JSON失败，保存原始数据: {}", e.getMessage());
            }

            // 保存数据
            radiationDeviceDataService.save(data);
            log.info("💾 辐射设备数据已保存: {}", device.getDeviceCode());

        } catch (Exception e) {
            log.error("❌ 处理辐射设备数据失败: {}", device.getDeviceCode(), e);
        }
    }

    /**
     * 处理环境设备数据
     */
    private void handleEnvironmentData(Device device, String payload) {
        try {
            log.debug("🌍 处理环境设备数据: {}", device.getDeviceCode());

            // 创建环境设备数据记录
            com.cdutetc.ems.entity.EnvironmentDeviceData data = new com.cdutetc.ems.entity.EnvironmentDeviceData();
            data.setDeviceCode(device.getDeviceCode());
            data.setRawData(payload);
            data.setRecordTime(LocalDateTime.now());

            // 尝试解析JSON数据
            try {
                // 这里可以添加JSON解析逻辑
                data.setSrc(1); // 默认值
                // 解析temperature、wetness等字段...
            } catch (Exception e) {
                log.warn("⚠️ 解析环境设备数据JSON失败，保存原始数据: {}", e.getMessage());
            }

            // 保存数据
            environmentDeviceDataService.save(data);
            log.info("💾 环境设备数据已保存: {}", device.getDeviceCode());

        } catch (Exception e) {
            log.error("❌ 处理环境设备数据失败: {}", device.getDeviceCode(), e);
        }
    }

    /**
     * 设备主题信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class DeviceTopicInfo {
        private String deviceCode;
        private String deviceType;
        private String originalTopic;
    }
}