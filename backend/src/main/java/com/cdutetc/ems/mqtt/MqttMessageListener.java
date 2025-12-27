package com.cdutetc.ems.mqtt;

import com.cdutetc.ems.config.CpmConversionProperties;
import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.cdutetc.ems.dto.mqtt.MqttDeviceDataMessage;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.service.AlertService;
import com.cdutetc.ems.service.DeviceService;
import com.cdutetc.ems.service.DeviceStatusCacheService;
import com.cdutetc.ems.service.EnvironmentDeviceDataService;
import com.cdutetc.ems.service.RadiationDeviceDataService;
import com.cdutetc.ems.service.SseEmitterService;
import com.cdutetc.ems.util.JsonParserUtil;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final DeviceStatusCacheService deviceStatusCacheService;
    private final ObjectMapper objectMapper;
    private final CpmConversionProperties cpmConversionProperties;

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

            // 验证设备是否存在且已激活
            Device device = getAndValidateDevice(topicInfo.getDeviceCode(), topicInfo.getDeviceType());

            // 如果设备验证失败，不处理数据
            if (device == null) {
                return;
            }

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
     * 获取并验证设备
     * 只处理已激活且归属企业的设备数据
     */
    private Device getAndValidateDevice(String deviceCode, String deviceTypeStr) {
        try {
            // 查找设备
            Device device = deviceService.findByDeviceCode(deviceCode);

            if (device == null) {
                log.warn("⚠️ 丢弃未录入设备 {} 的数据（设备不存在）", deviceCode);
                return null;
            }

            // 检查设备激活状态
            if (device.getActivationStatus() != DeviceActivationStatus.ACTIVE) {
                log.warn("⚠️ 丢弃未激活设备 {} 的数据（当前状态: {}）",
                    deviceCode, device.getActivationStatus());
                return null;
            }

            // 检查设备是否已归属企业
            if (device.getCompany() == null) {
                log.error("❌ 丢弃设备 {} 的数据（未归属企业）", deviceCode);
                return null;
            }

            // 更新设备状态缓存（最后消息时间和在线状态）
            deviceStatusCacheService.updateLastMessageTime(deviceCode, LocalDateTime.now());
            deviceStatusCacheService.updateStatus(deviceCode, "ONLINE");

            // 如果设备之前离线，自动解决离线告警
            try {
                alertService.resolveOfflineAlerts(deviceCode, device.getId());
            } catch (Exception e) {
                log.warn("解决离线告警失败: deviceCode={}, error={}", deviceCode, e.getMessage());
            }

            return device;

        } catch (Exception e) {
            log.error("❌ 验证设备失败: {}", deviceCode, e);
            return null;
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

            // 解析JSON数据（使用JsonParserUtil优化）
            try {
                JsonNode rootNode = objectMapper.readTree(payload);

                // 使用JsonParserUtil解析基础字段
                JsonParserUtil.parseInt(rootNode, "src").ifPresent(data::setSrc);
                JsonParserUtil.parseInt(rootNode, "msgtype").ifPresent(data::setMsgtype);

                // 解析CPM并应用转换系数
                JsonParserUtil.parseDouble(rootNode, "CPM").ifPresent(rawCpm -> {
                    double convertedCpm = cpmConversionProperties.isEnabled()
                        ? rawCpm / cpmConversionProperties.getRadiationConversionFactor()
                        : rawCpm;
                    data.setCpm(convertedCpm);
                    if (cpmConversionProperties.isEnabled()) {
                        log.debug("🔄 辐射设备CPM转换: 原始值={}, 转换系数={}, 转换后值={}",
                            rawCpm, cpmConversionProperties.getRadiationConversionFactor(), convertedCpm);
                    }
                });

                // 解析电池电压（辐射设备发送的是毫伏mV，需要转换为伏V存储）
                JsonParserUtil.parseDouble(rootNode, "Batvolt").ifPresent(rawBatvolt -> {
                    data.setBatvolt(rawBatvolt / 1000.0); // mV转V：原始值(mV) ÷ 1000 = 电压(V)
                    log.debug("🔄 辐射设备电压转换: 原始值={}mV, 转换后值={}V",
                        rawBatvolt, data.getBatvolt());
                });
                JsonParserUtil.parseString(rootNode, "time").ifPresent(data::setTime);
                JsonParserUtil.parseInt(rootNode, "trigger").ifPresent(data::setDataTrigger);
                JsonParserUtil.parseInt(rootNode, "multi").ifPresent(data::setMulti);
                JsonParserUtil.parseInt(rootNode, "way").ifPresent(data::setWay);

                // 解析BDS定位信息
                JsonParserUtil.parseObject(rootNode, "BDS").ifPresent(bds -> {
                    JsonParserUtil.parseString(bds, "longitude").ifPresent(data::setBdsLongitude);
                    JsonParserUtil.parseString(bds, "latitude").ifPresent(data::setBdsLatitude);
                    JsonParserUtil.parseString(bds, "UTC").ifPresent(data::setBdsUtc);
                    JsonParserUtil.parseInt(bds, "useful").ifPresent(data::setBdsUseful);
                });

                // 解析LBS定位信息
                JsonParserUtil.parseObject(rootNode, "LBS").ifPresent(lbs -> {
                    JsonParserUtil.parseString(lbs, "longitude").ifPresent(data::setLbsLongitude);
                    JsonParserUtil.parseString(lbs, "latitude").ifPresent(data::setLbsLatitude);
                    JsonParserUtil.parseInt(lbs, "useful").ifPresent(data::setLbsUseful);
                });

                log.debug("✅ 辐射数据解析成功: CPM={}, Batvolt={}, time={}",
                    data.getCpm(), data.getBatvolt(), data.getTime());

            } catch (Exception e) {
                log.warn("⚠️ 解析辐射设备数据JSON失败，仅保存原始数据: {}", e.getMessage());
            }

            // 保存数据
            com.cdutetc.ems.entity.RadiationDeviceData savedData = radiationDeviceDataService.save(data);
            log.info("💾 辐射设备数据已保存: {}", device.getDeviceCode());

            // 更新缓存：CPM值和电池电压
            if (savedData.getCpm() != null) {
                deviceStatusCacheService.updateLastCpm(device.getDeviceCode(), savedData.getCpm());
            }
            if (savedData.getBatvolt() != null) {
                deviceStatusCacheService.updateLastBattery(device.getDeviceCode(), savedData.getBatvolt());
            }

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
                // CPM上升率告警（辐射设备）
                alertService.checkRadiationDataAndAlert(
                    device.getDeviceCode(),
                    savedData.getCpm(),
                    "RADIATION",  // 辐射设备类型
                    device.getId(),
                    device.getCompany().getId()
                );

                // 电压告警（辐射设备）
                if (savedData.getBatvolt() != null) {
                    alertService.checkEnvironmentDataAndAlert(
                        device.getDeviceCode(),
                        savedData.getBatvolt(),
                        "RADIATION",  // 辐射设备类型
                        device.getId(),
                        device.getCompany().getId()
                    );
                }
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

            // 解析JSON数据（使用JsonParserUtil优化）
            try {
                JsonNode rootNode = objectMapper.readTree(payload);

                // 使用JsonParserUtil解析基础字段
                JsonParserUtil.parseInt(rootNode, "src").ifPresent(data::setSrc);

                // 解析CPM并应用转换系数
                JsonParserUtil.parseDouble(rootNode, "CPM").ifPresent(rawCpm -> {
                    double convertedCpm = cpmConversionProperties.isEnabled()
                        ? rawCpm / cpmConversionProperties.getEnvironmentConversionFactor()
                        : rawCpm;
                    data.setCpm(convertedCpm);
                    if (cpmConversionProperties.isEnabled()) {
                        log.debug("🔄 环境设备CPM转换: 原始值={}, 转换系数={}, 转换后值={}",
                            rawCpm, cpmConversionProperties.getEnvironmentConversionFactor(), convertedCpm);
                    }
                });

                JsonParserUtil.parseDouble(rootNode, "temperature").ifPresent(data::setTemperature);
                JsonParserUtil.parseDouble(rootNode, "wetness").ifPresent(data::setWetness);
                JsonParserUtil.parseDouble(rootNode, "windspeed").ifPresent(data::setWindspeed);
                JsonParserUtil.parseDouble(rootNode, "total").ifPresent(data::setTotal);
                JsonParserUtil.parseDouble(rootNode, "battery").ifPresent(data::setBattery);

                log.debug("✅ 环境数据解析成功: CPM={}, temperature={}, wetness={}, battery={}",
                    data.getCpm(), data.getTemperature(), data.getWetness(), data.getBattery());

            } catch (Exception e) {
                log.warn("⚠️ 解析环境设备数据JSON失败，仅保存原始数据: {}", e.getMessage());
            }

            // 保存数据
            com.cdutetc.ems.entity.EnvironmentDeviceData savedData = environmentDeviceDataService.save(data);
            log.info("💾 环境设备数据已保存: {}", device.getDeviceCode());

            // 更新缓存：电池电压
            if (savedData.getBattery() != null) {
                deviceStatusCacheService.updateLastBattery(device.getDeviceCode(), savedData.getBattery());
            }

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
                // CPM上升率告警（环境设备）
                if (savedData.getCpm() != null) {
                    alertService.checkRadiationDataAndAlert(
                        device.getDeviceCode(),
                        savedData.getCpm(),
                        "ENVIRONMENT",  // 环境设备类型
                        device.getId(),
                        device.getCompany().getId()
                    );
                }

                // 电压告警（环境设备）
                if (savedData.getBattery() != null) {
                    alertService.checkEnvironmentDataAndAlert(
                        device.getDeviceCode(),
                        savedData.getBattery(),
                        "ENVIRONMENT",  // 环境设备类型
                        device.getId(),
                        device.getCompany().getId()
                    );
                }
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