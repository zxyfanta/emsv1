package com.ems.service.mqtt;

import com.ems.entity.device.Device;
import com.ems.entity.DeviceStatusRecord;
import com.ems.service.AlertService;
import com.ems.service.BatchStorageService;
import com.ems.service.DeviceCacheService;
import com.ems.service.RealtimeCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 统一MQTT数据处理器
 * 解决重复数据处理问题，所有MQTT消息统一处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedMQTTDataProcessor {

    private final BatchStorageService batchStorageService;
    private final RealtimeCacheService realtimeCacheService;
    private final DeviceCacheService deviceCacheService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    /**
     * 统一处理所有MQTT消息
     * 不再区分/data、/status、/alert，统一为设备数据
     */
    public void processMessage(String topic, String payload) {
        try {
            String deviceId = extractDeviceIdFromTopic(topic);

            // 设备存在性验证
            if (!deviceCacheService.isDeviceActive(deviceId)) {
                log.warn("⚠️ 设备不存在或已禁用: {}", deviceId);
                return;
            }

            // 解析统一数据
            DeviceData data = parseDeviceData(payload, deviceId);
            if (data == null) {
                log.warn("⚠️ 数据解析失败: 设备={}, 主题={}", deviceId, topic);
                return;
            }

            // 统一处理流程
            processUnifiedData(data);

        } catch (Exception e) {
            log.error("❌ MQTT消息处理失败: 主题={}", topic, e);
        }
    }

    /**
     * 统一数据处理流程
     */
    private void processUnifiedData(DeviceData data) {
        LocalDateTime timestamp = LocalDateTime.now();

        // 1. 立即更新Redis实时缓存
        realtimeCacheService.updateRealtimeData(data, timestamp);

        // 2. 异步批量存储到MySQL
        DeviceStatusRecord record = convertToStatusRecord(data, timestamp);
        batchStorageService.addToBatch(record);

        // 3. 系统告警判断（基于统一数据）
        alertService.checkAlerts(data.getDeviceId(),
            data.getCpmValue() != null ? data.getCpmValue().doubleValue() : null,
            data.getBatteryVoltage(), timestamp);

        log.debug("✅ 统一数据处理完成: 设备={}, CPM={}, 电池={}mV",
                data.getDeviceId(), data.getCpmValue(), data.getBatteryVoltage());
    }

    /**
     * 解析设备数据
     */
    private DeviceData parseDeviceData(String payload, String deviceId) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            DeviceData data = new DeviceData();
            data.setDeviceId(deviceId);
            data.setCpmValue(extractIntValue(rootNode, "CPM"));
            data.setBatteryVoltage(extractIntValue(rootNode, "Batvolt"));
            data.setTriggerType(extractIntValue(rootNode, "trigger"));
            data.setTransmissionWay(extractIntValue(rootNode, "way"));
            data.setMultiFlag(extractIntValue(rootNode, "multi"));
            data.setMessageType(extractIntValue(rootNode, "msgtype"));
            data.setSourceFlag(extractIntValue(rootNode, "src"));
            data.setLocalTimeString(extractStringValue(rootNode, "time"));

            // 解析GPS位置信息
            parseGpsLocation(rootNode, data);

            return data;

        } catch (Exception e) {
            log.error("❌ 数据解析失败: 设备={}", deviceId, e);
            return null;
        }
    }

    /**
     * 转换为状态记录
     */
    private DeviceStatusRecord convertToStatusRecord(DeviceData data, LocalDateTime timestamp) {
        DeviceStatusRecord record = new DeviceStatusRecord();
        record.setCpmValue(data.getCpmValue());
        record.setBatteryVoltageMv(data.getBatteryVoltage());
        record.setRecordTime(timestamp);
        record.setDataSource("MQTT");
        record.setProcessingStatus("PROCESSED");

        // 设置设备关联（通过deviceCacheService获取）
        deviceCacheService.getDeviceInfo(data.getDeviceId())
            .ifPresent(deviceInfo -> {
                // 创建设备对象关联
                Device device = new Device();
                device.setId(deviceInfo.getId());
                device.setDeviceId(deviceInfo.getDeviceId());
                record.setDevice(device);
            });

        return record;
    }

    // 辅助方法
    private String extractDeviceIdFromTopic(String topic) {
        try {
            String[] parts = topic.replaceFirst("^/", "").split("/");
            if (parts.length >= 3 && "ems".equals(parts[0]) && "device".equals(parts[1])) {
                return parts[2];
            }
            return null;
        } catch (Exception e) {
            log.error("❌ 提取设备ID失败: 主题={}", topic, e);
            return null;
        }
    }

    private Integer extractIntValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asInt() : null;
    }

    private String extractStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    private void parseGpsLocation(JsonNode rootNode, DeviceData data) {
        // 解析BDS位置
        if (rootNode.has("BDS")) {
            JsonNode bdsNode = rootNode.get("BDS");
            data.setBdsLongitude(extractStringValue(bdsNode, "longitude"));
            data.setBdsLatitude(extractStringValue(bdsNode, "latitude"));
            data.setBdsUtc(extractStringValue(bdsNode, "UTC"));
            data.setBdsUseful(extractIntValue(bdsNode, "useful") == 1);
        }

        // 解析LBS位置
        if (rootNode.has("LBS")) {
            JsonNode lbsNode = rootNode.get("LBS");
            data.setLbsLongitude(extractStringValue(lbsNode, "longitude"));
            data.setLbsLatitude(extractStringValue(lbsNode, "latitude"));
            data.setLbsUseful(extractIntValue(lbsNode, "useful") == 1);
        }
    }

    /**
     * 设备数据统一模型
     */
    public static class DeviceData {
        private String deviceId;
        private Integer cpmValue;
        private Integer batteryVoltage;
        private Integer triggerType;
        private Integer transmissionWay;
        private Integer multiFlag;
        private Integer messageType;
        private Integer sourceFlag;
        private String localTimeString;

        // GPS位置信息
        private String bdsLongitude;
        private String bdsLatitude;
        private String bdsUtc;
        private Boolean bdsUseful;
        private String lbsLongitude;
        private String lbsLatitude;
        private Boolean lbsUseful;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public Integer getCpmValue() { return cpmValue; }
        public void setCpmValue(Integer cpmValue) { this.cpmValue = cpmValue; }
        public Integer getBatteryVoltage() { return batteryVoltage; }
        public void setBatteryVoltage(Integer batteryVoltage) { this.batteryVoltage = batteryVoltage; }
        public Integer getTriggerType() { return triggerType; }
        public void setTriggerType(Integer triggerType) { this.triggerType = triggerType; }
        public Integer getTransmissionWay() { return transmissionWay; }
        public void setTransmissionWay(Integer transmissionWay) { this.transmissionWay = transmissionWay; }
        public Integer getMultiFlag() { return multiFlag; }
        public void setMultiFlag(Integer multiFlag) { this.multiFlag = multiFlag; }
        public Integer getMessageType() { return messageType; }
        public void setMessageType(Integer messageType) { this.messageType = messageType; }
        public Integer getSourceFlag() { return sourceFlag; }
        public void setSourceFlag(Integer sourceFlag) { this.sourceFlag = sourceFlag; }
        public String getLocalTimeString() { return localTimeString; }
        public void setLocalTimeString(String localTimeString) { this.localTimeString = localTimeString; }
        public String getBdsLongitude() { return bdsLongitude; }
        public void setBdsLongitude(String bdsLongitude) { this.bdsLongitude = bdsLongitude; }
        public String getBdsLatitude() { return bdsLatitude; }
        public void setBdsLatitude(String bdsLatitude) { this.bdsLatitude = bdsLatitude; }
        public String getBdsUtc() { return bdsUtc; }
        public void setBdsUtc(String bdsUtc) { this.bdsUtc = bdsUtc; }
        public Boolean getBdsUseful() { return bdsUseful; }
        public void setBdsUseful(Boolean bdsUseful) { this.bdsUseful = bdsUseful; }
        public String getLbsLongitude() { return lbsLongitude; }
        public void setLbsLongitude(String lbsLongitude) { this.lbsLongitude = lbsLongitude; }
        public String getLbsLatitude() { return lbsLatitude; }
        public void setLbsLatitude(String lbsLatitude) { this.lbsLatitude = lbsLatitude; }
        public Boolean getLbsUseful() { return lbsUseful; }
        public void setLbsUseful(Boolean lbsUseful) { this.lbsUseful = lbsUseful; }
    }
}