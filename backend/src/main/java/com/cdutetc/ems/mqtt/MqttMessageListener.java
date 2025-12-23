package com.cdutetc.ems.mqtt;

import com.cdutetc.ems.config.MqttConfig;
import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.cdutetc.ems.dto.mqtt.MqttDeviceDataMessage;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.service.AlertService;
import com.cdutetc.ems.service.DeviceService;
import com.cdutetc.ems.service.EnvironmentDeviceDataService;
import com.cdutetc.ems.service.RadiationDeviceDataService;
import com.cdutetc.ems.service.SseEmitterService;
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
    private final SseEmitterService sseEmitterService;
    private final AlertService alertService;
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
     * 原始JSON格式: {"src":1,"msgtype":1,"CPM":123,"Batvolt":3989,"time":"2025/01/15 14:30:45","trigger":1,"multi":1,"way":1}
     */
    private void handleRadiationData(Device device, String payload) {
        try {
            log.debug("🔬 处理辐射设备数据: {}", device.getDeviceCode());

            // 创建辐射设备数据记录
            com.cdutetc.ems.entity.RadiationDeviceData data = new com.cdutetc.ems.entity.RadiationDeviceData();
            data.setDeviceCode(device.getDeviceCode());
            data.setRawData(payload);
            data.setRecordTime(LocalDateTime.now());

            // 解析JSON数据
            try {
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(payload);

                // 解析基础字段 - 注意JSON字段名与entity属性名的对应关系
                if (rootNode.has("src")) {
                    data.setSrc(rootNode.get("src").asInt());
                }
                if (rootNode.has("msgtype")) {
                    data.setMsgtype(rootNode.get("msgtype").asInt());
                }
                if (rootNode.has("CPM")) {
                    data.setCpm(rootNode.get("CPM").asDouble());
                }
                if (rootNode.has("Batvolt")) {
                    data.setBatvolt(rootNode.get("Batvolt").asDouble());
                }
                if (rootNode.has("time")) {
                    data.setTime(rootNode.get("time").asText());
                }
                if (rootNode.has("trigger")) {
                    data.setTrigger(rootNode.get("trigger").asInt());
                }
                if (rootNode.has("multi")) {
                    data.setMulti(rootNode.get("multi").asInt());
                }
                if (rootNode.has("way")) {
                    data.setWay(rootNode.get("way").asInt());
                }

                // 解析BDS定位信息
                if (rootNode.has("BDS") && rootNode.get("BDS").isObject()) {
                    com.fasterxml.jackson.databind.JsonNode bds = rootNode.get("BDS");
                    if (bds.has("longitude")) {
                        data.setBdsLongitude(bds.get("longitude").asText());
                    }
                    if (bds.has("latitude")) {
                        data.setBdsLatitude(bds.get("latitude").asText());
                    }
                    if (bds.has("UTC")) {
                        data.setBdsUtc(bds.get("UTC").asText());
                    }
                    if (bds.has("useful")) {
                        data.setBdsUseful(bds.get("useful").asInt());
                    }
                }

                // 解析LBS定位信息
                if (rootNode.has("LBS") && rootNode.get("LBS").isObject()) {
                    com.fasterxml.jackson.databind.JsonNode lbs = rootNode.get("LBS");
                    if (lbs.has("longitude")) {
                        data.setLbsLongitude(lbs.get("longitude").asText());
                    }
                    if (lbs.has("latitude")) {
                        data.setLbsLatitude(lbs.get("latitude").asText());
                    }
                    if (lbs.has("useful")) {
                        data.setLbsUseful(lbs.get("useful").asInt());
                    }
                }

                log.debug("✅ 辐射数据解析成功: CPM={}, Batvolt={}, time={}",
                    data.getCpm(), data.getBatvolt(), data.getTime());

            } catch (Exception e) {
                log.warn("⚠️ 解析辐射设备数据JSON失败，仅保存原始数据: {}", e.getMessage());
            }

            // 保存数据
            com.cdutetc.ems.entity.RadiationDeviceData savedData = radiationDeviceDataService.save(data);
            log.info("💾 辐射设备数据已保存: {}", device.getDeviceCode());

            // SSE推送实时数据
            try {
                DeviceDataEvent event = new DeviceDataEvent(
                    "radiation-data",
                    device.getDeviceCode(),
                    "RADIATION_MONITOR",
                    java.util.Map.of(
                        "cpm", savedData.getCpm(),
                        "batVolt", savedData.getBatvolt(),
                        "recordTime", savedData.getRecordTime().toString()
                    )
                );
                sseEmitterService.broadcastDeviceData(device.getCompany().getId(), event);
                log.debug("📡 SSE推送辐射数据成功: {}", device.getDeviceCode());
            } catch (Exception e) {
                log.warn("⚠️ SSE推送辐射数据失败: {}", e.getMessage());
            }

            // 检查告警条件
            try {
                alertService.checkRadiationDataAndAlert(
                    device.getDeviceCode(),
                    savedData.getCpm(),
                    device.getId(),
                    device.getCompany().getId()
                );
            } catch (Exception e) {
                log.warn("⚠️ 辐射数据告警检查失败: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ 处理辐射设备数据失败: {}", device.getDeviceCode(), e);
        }
    }

    /**
     * 处理环境设备数据
     * 原始JSON格式: {"src":1,"CPM":4,"temperature":10,"wetness":95,"windspeed":0.2,"total":144.1,"battery":11.9}
     */
    private void handleEnvironmentData(Device device, String payload) {
        try {
            log.debug("🌍 处理环境设备数据: {}", device.getDeviceCode());

            // 创建环境设备数据记录
            com.cdutetc.ems.entity.EnvironmentDeviceData data = new com.cdutetc.ems.entity.EnvironmentDeviceData();
            data.setDeviceCode(device.getDeviceCode());
            data.setRawData(payload);
            data.setRecordTime(LocalDateTime.now());

            // 解析JSON数据
            try {
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(payload);

                // 解析基础字段 - 注意JSON字段名与entity属性名的对应关系
                if (rootNode.has("src")) {
                    data.setSrc(rootNode.get("src").asInt());
                }
                if (rootNode.has("CPM")) {
                    data.setCpm(rootNode.get("CPM").asDouble());
                }
                if (rootNode.has("temperature")) {
                    data.setTemperature(rootNode.get("temperature").asDouble());
                }
                if (rootNode.has("wetness")) {
                    data.setWetness(rootNode.get("wetness").asDouble());
                }
                if (rootNode.has("windspeed")) {
                    data.setWindspeed(rootNode.get("windspeed").asDouble());
                }
                if (rootNode.has("total")) {
                    data.setTotal(rootNode.get("total").asDouble());
                }
                if (rootNode.has("battery")) {
                    data.setBattery(rootNode.get("battery").asDouble());
                }

                log.debug("✅ 环境数据解析成功: CPM={}, temperature={}, wetness={}, battery={}",
                    data.getCpm(), data.getTemperature(), data.getWetness(), data.getBattery());

            } catch (Exception e) {
                log.warn("⚠️ 解析环境设备数据JSON失败，仅保存原始数据: {}", e.getMessage());
            }

            // 保存数据
            com.cdutetc.ems.entity.EnvironmentDeviceData savedData = environmentDeviceDataService.save(data);
            log.info("💾 环境设备数据已保存: {}", device.getDeviceCode());

            // SSE推送实时数据
            try {
                DeviceDataEvent event = new DeviceDataEvent(
                    "environment-data",
                    device.getDeviceCode(),
                    "ENVIRONMENT_STATION",
                    java.util.Map.of(
                        "cpm", savedData.getCpm(),
                        "temperature", savedData.getTemperature(),
                        "wetness", savedData.getWetness(),
                        "windspeed", savedData.getWindspeed(),
                        "recordTime", savedData.getRecordTime().toString()
                    )
                );
                sseEmitterService.broadcastDeviceData(device.getCompany().getId(), event);
                log.debug("📡 SSE推送环境数据成功: {}", device.getDeviceCode());
            } catch (Exception e) {
                log.warn("⚠️ SSE推送环境数据失败: {}", e.getMessage());
            }

            // 检查告警条件
            try {
                alertService.checkEnvironmentDataAndAlert(
                    device.getDeviceCode(),
                    savedData.getBattery(),
                    device.getId(),
                    device.getCompany().getId()
                );
            } catch (Exception e) {
                log.warn("⚠️ 环境数据告警检查失败: {}", e.getMessage());
            }

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