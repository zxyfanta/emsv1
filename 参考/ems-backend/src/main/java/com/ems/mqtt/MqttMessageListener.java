package com.ems.mqtt;

import com.ems.service.AlertService;
import com.ems.service.DeviceDataService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MQTT消息监听器
 * 监听设备数据主题并处理接收到的消息
 *
 * @author EMS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageListener implements MqttCallback {

    private final DeviceDataService deviceDataService;
    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        try {
            String payload = new String(mqttMessage.getPayload());
            log.info("📥 收到MQTT消息 - 主题: {}, 消息: {}", topic, payload);

            // 从主题中提取设备ID
            String deviceId = extractDeviceIdFromTopic(topic);
            log.info("📍 设备ID: {}", deviceId);

            // 根据主题类型处理不同格式的消息
            if (topic.endsWith("/data")) {
                // 处理GPS追踪器数据
                handleGpsTrackerData(deviceId, payload, topic);
            } else if (topic.endsWith("/status")) {
                // 处理设备状态消息
                handleDeviceStatus(deviceId, payload, topic);
            } else if (topic.endsWith("/alert")) {
                // 处理设备告警消息
                handleDeviceAlert(deviceId, payload, topic);
            } else {
                log.warn("⚠️ 未知的MQTT主题格式: {}", topic);
            }

        } catch (Exception e) {
            log.error("❌ 处理MQTT消息失败 - 主题: {}, 错误: {}", topic, e.getMessage(), e);
        }
    }

    /**
     * 处理GPS追踪器数据
     */
    private void handleGpsTrackerData(String deviceId, String payload, String topic) {
        try {
            // 解析GPS设备数据
            GpsTrackerMessage gpsData = objectMapper.readValue(payload, GpsTrackerMessage.class);
            log.info("📊 解析GPS数据: 源={}, 类型={}, CPM={}, 电池={}mV, BDS有效={}, LBS有效={}",
                    gpsData.getSrc(), gpsData.getMsgtype(), gpsData.getCpm(),
                    gpsData.getBatvolt(),
                    gpsData.getBds() != null ? gpsData.getBds().getUseful() : 0,
                    gpsData.getLbs() != null ? gpsData.getLbs().getUseful() : 0);

            // 转换为设备数据格式并保存
            DeviceDataMessage deviceData = convertToDeviceData(deviceId, gpsData);
            deviceDataService.saveDeviceData(deviceData);
            log.info("💾 GPS数据已保存到数据库");

            // 新增：触发告警检测
            if (gpsData.getCpm() != null || gpsData.getBatvolt() != null) {
                Integer batteryMv = gpsData.getBatvolt() != null ? gpsData.getBatvolt() : null;
                alertService.checkAlerts(
                    deviceId,
                    gpsData.getCpm().doubleValue(),
                    batteryMv,
                    LocalDateTime.now()
                );
                log.info("🚨 GPS数据告警检测完成: CPM={}, 电池={}mV", gpsData.getCpm(), batteryMv);
            }

        } catch (Exception e) {
            log.error("❌ 解析GPS数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理设备状态消息
     */
    private void handleDeviceStatus(String deviceId, String payload, String topic) {
        try {
            // 解析设备状态数据
            DeviceStatusMessage statusData = objectMapper.readValue(payload, DeviceStatusMessage.class);
            log.info("📊 解析设备状态: 设备ID={}, 类型={}, 在线={}, 电池={}%, 信号={}dBm",
                    statusData.getDeviceId(), statusData.getDeviceType(),
                    statusData.getStatus().getOnline(),
                    statusData.getStatus().getBatteryLevel(),
                    statusData.getStatus().getSignalQuality());

            // 这里可以将状态数据保存到状态表或进行其他处理
            log.info("💾 设备状态已处理");

        } catch (Exception e) {
            log.error("❌ 解析设备状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理设备告警消息
     */
    private void handleDeviceAlert(String deviceId, String payload, String topic) {
        try {
            // 解析设备告警数据
            DeviceAlertMessage alertData = objectMapper.readValue(payload, DeviceAlertMessage.class);
            if (alertData.getAlert() != null) {
                log.info("🚨 解析设备告警: 设备ID={}, 类型={}, 级别={}, 消息={}",
                        alertData.getDeviceId(), alertData.getAlert().getType(),
                        alertData.getAlert().getLevel(), alertData.getAlert().getMessage());
            } else {
                log.info("ℹ️ 设备{}运行正常，无告警", alertData.getDeviceId());
            }

            // 这里可以将告警数据保存到告警表或发送通知
            log.info("💾 设备告警已处理");

        } catch (Exception e) {
            log.error("❌ 解析设备告警失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从主题中提取设备ID
     */
    private String extractDeviceIdFromTopic(String topic) {
        // 主题格式: ems/device/{deviceId}/data 或 ems/device/{deviceId}/status
        String[] parts = topic.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }

    /**
     * 将GPS追踪器消息转换为设备数据格式
     */
    private DeviceDataMessage convertToDeviceData(String deviceId, GpsTrackerMessage gpsData) {
        DeviceDataMessage deviceData = new DeviceDataMessage();
        deviceData.setDeviceId(deviceId);
        deviceData.setTimestamp(System.currentTimeMillis());
        deviceData.setTime(gpsData.getTime());
        deviceData.setCpm(gpsData.getCpm());
        deviceData.setBattery(String.valueOf(gpsData.getBatvolt()));

        // 设置位置信息 (优先使用BDS，如果无效则使用LBS)
        Location location = new Location();
        if (gpsData.getBds().getUseful() == 1) {
            // 转换北斗格式坐标为十进制
            location.setLongitude(convertBdsToDecimal(gpsData.getBds().getLongitude()));
            location.setLatitude(convertBdsToDecimal(gpsData.getBds().getLatitude()));
        } else if (gpsData.getLbs().getUseful() == 1) {
            location.setLongitude(Double.parseDouble(gpsData.getLbs().getLongitude()));
            location.setLatitude(Double.parseDouble(gpsData.getLbs().getLatitude()));
        }
        deviceData.setLocation(location);

        deviceData.setStatus("online");
        return deviceData;
    }

    /**
     * 将北斗度分格式坐标转换为十进制格式
     */
    private Double convertBdsToDecimal(String bdsCoord) {
        try {
            // 北斗格式: DDDMM.mmmm (度分格式)
            String coord = bdsCoord.replace("\"", "");
            int degrees = Integer.parseInt(coord.substring(0, 3));
            double minutes = Double.parseDouble(coord.substring(3));
            return degrees + minutes / 60.0;
        } catch (Exception e) {
            log.warn("坐标转换失败: {}", bdsCoord);
            return 0.0;
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT连接丢失: {}", cause.getMessage(), cause);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            log.debug("MQTT消息发送完成: {}", token.getMessage());
        } catch (Exception e) {
            log.debug("MQTT消息发送完成记录出错: {}", e.getMessage());
        }
    }

    /**
     * GPS追踪器消息实体 (基于实际设备数据格式)
     */
    public static class GpsTrackerMessage {
        private Integer src;
        private Integer msgtype;
        @JsonProperty("CPM")
        private Integer cpm;
        @JsonProperty("Batvolt")
        private Integer batvolt;
        @JsonProperty("BDS")
        private BdsData bds;
        @JsonProperty("LBS")
        private LbsData lbs;
        private String time;
        private Integer trigger;
        private Integer multi;
        private Integer way;

        // Getters and Setters
        public Integer getSrc() { return src; }
        public void setSrc(Integer src) { this.src = src; }

        public Integer getMsgtype() { return msgtype; }
        public void setMsgtype(Integer msgtype) { this.msgtype = msgtype; }

        public Integer getCpm() { return cpm; }
        public void setCpm(Integer cpm) { this.cpm = cpm; }

        public Integer getBatvolt() { return batvolt; }
        public void setBatvolt(Integer batvolt) { this.batvolt = batvolt; }

        public BdsData getBds() { return bds; }
        public void setBds(BdsData bds) { this.bds = bds; }

        public LbsData getLbs() { return lbs; }
        public void setLbs(LbsData lbs) { this.lbs = lbs; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public Integer getTrigger() { return trigger; }
        public void setTrigger(Integer trigger) { this.trigger = trigger; }

        public Integer getMulti() { return multi; }
        public void setMulti(Integer multi) { this.multi = multi; }

        public Integer getWay() { return way; }
        public void setWay(Integer way) { this.way = way; }
    }

    /**
     * BDS北斗定位数据
     */
    public static class BdsData {
        private String longitude;
        private String latitude;
        private String utc;
        private Integer useful;

        // Getters and Setters
        public String getLongitude() { return longitude; }
        public void setLongitude(String longitude) { this.longitude = longitude; }

        public String getLatitude() { return latitude; }
        public void setLatitude(String latitude) { this.latitude = latitude; }

        public String getUtc() { return utc; }
        public void setUtc(String utc) { this.utc = utc; }

        public Integer getUseful() { return useful; }
        public void setUseful(Integer useful) { this.useful = useful; }
    }

    /**
     * LBS基站定位数据
     */
    public static class LbsData {
        private String longitude;
        private String latitude;
        private Integer useful;

        // Getters and Setters
        public String getLongitude() { return longitude; }
        public void setLongitude(String longitude) { this.longitude = longitude; }

        public String getLatitude() { return latitude; }
        public void setLatitude(String latitude) { this.latitude = latitude; }

        public Integer getUseful() { return useful; }
        public void setUseful(Integer useful) { this.useful = useful; }
    }

    /**
     * 设备数据消息实体 (用于存储到数据库)
     */
    public static class DeviceDataMessage {
        private String deviceId;
        private Long timestamp;
        private String time;
        private Integer cpm;
        private String battery;
        private Location location;
        private String status;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public Integer getCpm() { return cpm; }
        public void setCpm(Integer cpm) { this.cpm = cpm; }

        public String getBattery() { return battery; }
        public void setBattery(String battery) { this.battery = battery; }

        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 位置信息
     */
    public static class Location {
        private Double longitude;
        private Double latitude;

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
    }

    /**
     * 设备状态消息实体
     */
    public static class DeviceStatusMessage {
        private String deviceId;
        private String deviceType;
        private String timestamp;
        private StatusInfo status;
        private LocationInfo location;
        private HealthInfo health;
        private MetadataInfo metadata;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public StatusInfo getStatus() { return status; }
        public void setStatus(StatusInfo status) { this.status = status; }

        public LocationInfo getLocation() { return location; }
        public void setLocation(LocationInfo location) { this.location = location; }

        public HealthInfo getHealth() { return health; }
        public void setHealth(HealthInfo health) { this.health = health; }

        public MetadataInfo getMetadata() { return metadata; }
        public void setMetadata(MetadataInfo metadata) { this.metadata = metadata; }
    }

    /**
     * 设备告警消息实体
     */
    public static class DeviceAlertMessage {
        private String deviceId;
        private String deviceType;
        private String timestamp;
        private AlertInfo alert;
        private String message; // 正常状态时的消息

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public AlertInfo getAlert() { return alert; }
        public void setAlert(AlertInfo alert) { this.alert = alert; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 状态信息
     */
    public static class StatusInfo {
        private Boolean online;
        private String error;
        private Long uptime;
        private Integer batteryLevel;
        private Integer signalQuality;
        private String lastReboot;

        // Getters and Setters
        public Boolean getOnline() { return online; }
        public void setOnline(Boolean online) { this.online = online; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public Long getUptime() { return uptime; }
        public void setUptime(Long uptime) { this.uptime = uptime; }

        public Integer getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

        public Integer getSignalQuality() { return signalQuality; }
        public void setSignalQuality(Integer signalQuality) { this.signalQuality = signalQuality; }

        public String getLastReboot() { return lastReboot; }
        public void setLastReboot(String lastReboot) { this.lastReboot = lastReboot; }
    }

    /**
     * 位置信息详情
     */
    public static class LocationInfo {
        private String lastUpdate;
        private Integer accuracy;
        private Integer satellites;
        private String fixType;

        // Getters and Setters
        public String getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }

        public Integer getAccuracy() { return accuracy; }
        public void setAccuracy(Integer accuracy) { this.accuracy = accuracy; }

        public Integer getSatellites() { return satellites; }
        public void setSatellites(Integer satellites) { this.satellites = satellites; }

        public String getFixType() { return fixType; }
        public void setFixType(String fixType) { this.fixType = fixType; }
    }

    /**
     * 健康信息
     */
    public static class HealthInfo {
        private String overall;
        private Boolean temperatureNormal;
        private Boolean humidityNormal;
        private Boolean vibrationNormal;

        // Getters and Setters
        public String getOverall() { return overall; }
        public void setOverall(String overall) { this.overall = overall; }

        public Boolean getTemperatureNormal() { return temperatureNormal; }
        public void setTemperatureNormal(Boolean temperatureNormal) { this.temperatureNormal = temperatureNormal; }

        public Boolean getHumidityNormal() { return humidityNormal; }
        public void setHumidityNormal(Boolean humidityNormal) { this.humidityNormal = humidityNormal; }

        public Boolean getVibrationNormal() { return vibrationNormal; }
        public void setVibrationNormal(Boolean vibrationNormal) { this.vibrationNormal = vibrationNormal; }
    }

    /**
     * 元数据信息
     */
    public static class MetadataInfo {
        private String firmwareVersion;
        private String hardwareVersion;
        private String manufacturer;
        private String model;
        private String serialNumber;
        private String protocol;

        // Getters and Setters
        public String getFirmwareVersion() { return firmwareVersion; }
        public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

        public String getHardwareVersion() { return hardwareVersion; }
        public void setHardwareVersion(String hardwareVersion) { this.hardwareVersion = hardwareVersion; }

        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
    }

    /**
     * 告警信息
     */
    public static class AlertInfo {
        private String id;
        private String type;
        private String level;
        private String message;
        private AlertLocation location;
        private Double value;
        private Double threshold;
        private Boolean acknowledged;
        private Boolean resolved;
        private String createdAt;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public AlertLocation getLocation() { return location; }
        public void setLocation(AlertLocation location) { this.location = location; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public Double getThreshold() { return threshold; }
        public void setThreshold(Double threshold) { this.threshold = threshold; }

        public Boolean getAcknowledged() { return acknowledged; }
        public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }

        public Boolean getResolved() { return resolved; }
        public void setResolved(Boolean resolved) { this.resolved = resolved; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    /**
     * 告警位置信息
     */
    public static class AlertLocation {
        private Double latitude;
        private Double longitude;

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
}