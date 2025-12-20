package com.ems.service.mqtt;

import com.ems.entity.device.Device;
import com.ems.entity.DeviceType;
import com.ems.service.AlertService;
import com.ems.service.DataPersistenceService;
import com.ems.repository.device.DeviceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * MQTT消息接收器（EMQX版本）
 * 负责接收来自EMQX的设备数据并存储到MySQL + Redis
 * 注意：设备认证和授权已在EMQX Broker层处理，无需在此验证设备存在性
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttMessageReceiver implements MqttCallback {

    private final DeviceRepository deviceRepository;
    private final GpsDataParser gpsDataParser;
    private final DataPersistenceService dataPersistenceService;
    private final AlertService alertService;
    private final DeviceTypeRouter deviceTypeRouter;
    private final ObjectMapper objectMapper;

    @Override
    public void connectionLost(Throwable cause) {
        log.error("❌ MQTT连接断开", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());
            log.info("📨 收到MQTT消息: 主题={}, 长度={}字节", topic, payload.length());

            // 🆕 使用方案一主题结构: ems/device/{deviceId}/data/{deviceType}
            String deviceId = DeviceType.extractDeviceIdFromTopic(topic);
            String deviceTypeCode = DeviceType.extractDeviceTypeFromTopic(topic);

            if (deviceId == null || deviceId.isEmpty()) {
                log.warn("⚠️ 无法从主题提取设备ID: {}", topic);
                return;
            }

            if (deviceTypeCode == null || deviceTypeCode.isEmpty()) {
                log.warn("⚠️ 无法从主题提取设备类型: {}", topic);
                return;
            }

            log.debug("🔍 从主题解析: 设备ID={}, 设备类型={}", deviceId, deviceTypeCode);

            // 注意：由于EMQX已经验证了设备的认证和授权，这里无需再次验证设备存在性
            // 如果能收到消息，说明设备一定是已注册的

            // 获取设备信息（用于后续处理）
            Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);
            if (device == null) {
                log.warn("⚠️ 设备在数据库中不存在，但通过了EMQX认证: {}", deviceId);
                // 创建临时设备对象用于处理，避免数据丢失
                device = new Device();
                device.setDeviceId(deviceId);
                device.setDeviceName("临时设备-" + deviceId);
            }

            // 更新设备在线状态
            device.setLastOnlineAt(LocalDateTime.now());
            device.setStatus(Device.DeviceStatus.ONLINE);
            deviceRepository.save(device);

            // 🆕 使用设备类型路由器处理数据，传递设备类型
            deviceTypeRouter.routeMessageWithDeviceType(deviceId, deviceTypeCode, topic, payload);

        } catch (Exception e) {
            log.error("❌ 处理MQTT消息失败: 主题={}", topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("✅ MQTT消息发送完成");
    }

  
    /**
     * 从MQTT主题中提取设备ID
     * 支持多种主题格式: ems/device/{deviceId}/data, ems/device/{deviceId}/status, /ems/device/{deviceId}/data
     */
    private String extractDeviceIdFromTopic(String topic) {
        try {
            log.debug("🔍 解析MQTT主题: {}", topic);

            // 移除开头的斜杠并分割
            String[] parts = topic.replaceFirst("^/", "").split("/");

            // 主题格式应为: ems/device/{deviceId}/{messageType}
            // parts[0] = "ems", parts[1] = "device", parts[2] = "{deviceId}", parts[3] = "data/status/alert"

            if (parts.length >= 3 && "ems".equals(parts[0]) && "device".equals(parts[1])) {
                String deviceId = parts[2];
                log.debug("📋 从主题中提取到设备ID: {}", deviceId);
                return deviceId;
            }

            // 兼容其他格式：ems/gps/{deviceId}/data
            if (parts.length >= 3 && "ems".equals(parts[0])) {
                String deviceId = parts[2];
                log.debug("📋 从兼容主题中提取到设备ID: {}", deviceId);
                return deviceId;
            }

            log.warn("⚠️ 无法识别的主题格式: {}", topic);
            return null;

        } catch (Exception e) {
            log.error("❌ 提取设备ID失败: 主题={}", topic, e);
            return null;
        }
    }
}