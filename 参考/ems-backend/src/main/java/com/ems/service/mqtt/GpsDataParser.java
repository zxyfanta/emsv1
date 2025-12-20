package com.ems.service.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * GPS数据解析器
 * 负责解析来自Node-Red的真实GPS数据格式
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpsDataParser {

    private final ObjectMapper objectMapper;

    // 时间格式模式
    private static final Pattern UTC_TIME_PATTERN = Pattern.compile("^\\d{6}\\.\\d{2}$");
    private static final Pattern LOCAL_TIME_PATTERN = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 解析GPS数据
     */
    public GpsDataPoint parseGpsData(String jsonData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);

            GpsDataPoint dataPoint = new GpsDataPoint();

            // 解析基础字段
            dataPoint.setSrc(rootNode.get("src").asInt());
            dataPoint.setMsgType(rootNode.get("msgtype").asInt());
            dataPoint.setCpm(rootNode.get("CPM").asInt());
            dataPoint.setBatteryVoltage(rootNode.get("Batvolt").asInt());
            dataPoint.setLocalTime(rootNode.get("time").asText());
            dataPoint.setTrigger(rootNode.get("trigger").asInt());
            dataPoint.setMulti(rootNode.get("multi").asInt());
            dataPoint.setWay(rootNode.get("way").asInt());

            // 解析时间戳
            dataPoint.setTimestamp(parseTimestamp(rootNode));

            // 解析BDS定位信息
            JsonNode bdsNode = rootNode.get("BDS");
            if (bdsNode != null) {
                BdsLocation bdsLocation = parseBdsLocation(bdsNode);
                dataPoint.setBdsLocation(bdsLocation);
            }

            // 解析LBS定位信息
            JsonNode lbsNode = rootNode.get("LBS");
            if (lbsNode != null) {
                LbsLocation lbsLocation = parseLbsLocation(lbsNode);
                dataPoint.setLbsLocation(lbsLocation);
            }

            // 选择主要定位信息
            LocationInfo primaryLocation = selectPrimaryLocation(
                dataPoint.getBdsLocation(), dataPoint.getLbsLocation());
            dataPoint.setPrimaryLocation(primaryLocation);

            log.debug("GPS数据解析完成: CPM={}, 电池={}mV, 定位类型={}",
                     dataPoint.getCpm(), dataPoint.getBatteryVoltage(),
                     primaryLocation != null ? primaryLocation.getType() : "无");

            return dataPoint;

        } catch (Exception e) {
            log.error("❌ GPS数据解析失败: {}", jsonData, e);
            return null;
        }
    }

    /**
     * 解析时间戳
     */
    private Instant parseTimestamp(JsonNode rootNode) {
        try {
            // 优先使用本地时间字段
            if (rootNode.has("time")) {
                String localTimeStr = rootNode.get("time").asText();
                if (LOCAL_TIME_PATTERN.matcher(localTimeStr).matches()) {
                    LocalDateTime localDateTime = LocalDateTime.parse(
                        localTimeStr, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                    return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
                }
            }

            // 备用：使用当前时间
            return Instant.now();

        } catch (Exception e) {
            log.warn("时间戳解析失败，使用当前时间", e);
            return Instant.now();
        }
    }

    /**
     * 解析BDS定位信息
     */
    private BdsLocation parseBdsLocation(JsonNode bdsNode) {
        try {
            BdsLocation bdsLocation = new BdsLocation();

            bdsLocation.setLongitude(bdsNode.get("longitude").asText());
            bdsLocation.setLatitude(bdsNode.get("latitude").asText());
            bdsLocation.setUtc(bdsNode.get("UTC").asText());
            bdsLocation.setUseful(bdsNode.get("useful").asInt() == 1);

            // 转换坐标
            if (bdsLocation.isUseful()) {
                try {
                    double longitudeDeg = convertBdsToDecimal(Double.parseDouble(bdsLocation.getLongitude()));
                    double latitudeDeg = convertBdsToDecimal(Double.parseDouble(bdsLocation.getLatitude()));
                    bdsLocation.setLongitudeDecimal(longitudeDeg);
                    bdsLocation.setLatitudeDecimal(latitudeDeg);
                } catch (Exception e) {
                    log.warn("BDS坐标转换失败: {}, {}", bdsLocation.getLongitude(), bdsLocation.getLatitude());
                    bdsLocation.setUseful(false);
                }
            }

            return bdsLocation;

        } catch (Exception e) {
            log.warn("BDS定位信息解析失败", e);
            return null;
        }
    }

    /**
     * 解析LBS定位信息
     */
    private LbsLocation parseLbsLocation(JsonNode lbsNode) {
        try {
            LbsLocation lbsLocation = new LbsLocation();

            lbsLocation.setLongitude(lbsNode.get("longitude").asDouble());
            lbsLocation.setLatitude(lbsNode.get("latitude").asDouble());
            lbsLocation.setUseful(lbsNode.get("useful").asInt() == 1);

            return lbsLocation;

        } catch (Exception e) {
            log.warn("LBS定位信息解析失败", e);
            return null;
        }
    }

    /**
     * 选择主要定位信息（优先BDS，备用LBS）
     */
    private LocationInfo selectPrimaryLocation(BdsLocation bdsLocation, LbsLocation lbsLocation) {
        // 优先使用BDS定位
        if (bdsLocation != null && bdsLocation.isUseful() &&
            bdsLocation.getLongitudeDecimal() != null && bdsLocation.getLatitudeDecimal() != null) {
            return new LocationInfo(
                "BDS",
                bdsLocation.getLongitudeDecimal(),
                bdsLocation.getLatitudeDecimal(),
                10.0 // BDS精度约10米
            );
        }

        // 备用LBS定位
        if (lbsLocation != null && lbsLocation.isUseful()) {
            return new LocationInfo(
                "LBS",
                lbsLocation.getLongitude(),
                lbsLocation.getLatitude(),
                100.0 // LBS精度约100米
            );
        }

        return null;
    }

    /**
     * 北斗坐标转换：度分格式 → 十进制
     */
    private Double convertBdsToDecimal(Double bdsCoordinate) {
        try {
            Double degrees = Math.floor(bdsCoordinate / 100);
            Double minutes = bdsCoordinate % 100;
            return degrees + minutes / 60;
        } catch (Exception e) {
            log.warn("北斗坐标转换失败: {}", bdsCoordinate, e);
            return null;
        }
    }

    /**
     * 验证UTC时间格式
     */
    public boolean isValidUtcTime(String utcTime) {
        return utcTime != null && UTC_TIME_PATTERN.matcher(utcTime).matches() &&
               !utcTime.equals("888888.00"); // 排除无效值
    }

    /**
     * 计算电池电量百分比
     */
    public Double calculateBatteryPercentage(Integer batteryVoltageMv) {
        if (batteryVoltageMv == null) {
            return null;
        }
        // 假设电压范围：3600mV-4000mV 对应 0%-100%
        return Math.max(0.0, Math.min(100.0, (batteryVoltageMv - 3600.0) / 400.0 * 100));
    }

    // 内部数据类

    /**
     * GPS数据点
     */
    public static class GpsDataPoint {
        private Integer src;
        private Integer msgType;
        private Integer cpm;
        private Integer batteryVoltage;
        private String localTime;
        private Integer trigger;
        private Integer multi;
        private Integer way;
        private Instant timestamp;
        private BdsLocation bdsLocation;
        private LbsLocation lbsLocation;
        private LocationInfo primaryLocation;

        // Getters and Setters
        public Integer getSrc() { return src; }
        public void setSrc(Integer src) { this.src = src; }
        public Integer getMsgType() { return msgType; }
        public void setMsgType(Integer msgType) { this.msgType = msgType; }
        public Integer getCpm() { return cpm; }
        public void setCpm(Integer cpm) { this.cpm = cpm; }
        public Integer getBatteryVoltage() { return batteryVoltage; }
        public void setBatteryVoltage(Integer batteryVoltage) { this.batteryVoltage = batteryVoltage; }
        public String getLocalTime() { return localTime; }
        public void setLocalTime(String localTime) { this.localTime = localTime; }
        public Integer getTrigger() { return trigger; }
        public void setTrigger(Integer trigger) { this.trigger = trigger; }
        public Integer getMulti() { return multi; }
        public void setMulti(Integer multi) { this.multi = multi; }
        public Integer getWay() { return way; }
        public void setWay(Integer way) { this.way = way; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public BdsLocation getBdsLocation() { return bdsLocation; }
        public void setBdsLocation(BdsLocation bdsLocation) { this.bdsLocation = bdsLocation; }
        public LbsLocation getLbsLocation() { return lbsLocation; }
        public void setLbsLocation(LbsLocation lbsLocation) { this.lbsLocation = lbsLocation; }
        public LocationInfo getPrimaryLocation() { return primaryLocation; }
        public void setPrimaryLocation(LocationInfo primaryLocation) { this.primaryLocation = primaryLocation; }
    }

    /**
     * BDS定位信息
     */
    public static class BdsLocation {
        private String longitude;      // 度分格式
        private String latitude;       // 度分格式
        private String utc;           // UTC时间
        private boolean useful;       // 是否有效
        private Double longitudeDecimal;  // 十进制
        private Double latitudeDecimal;   // 十进制

        // Getters and Setters
        public String getLongitude() { return longitude; }
        public void setLongitude(String longitude) { this.longitude = longitude; }
        public String getLatitude() { return latitude; }
        public void setLatitude(String latitude) { this.latitude = latitude; }
        public String getUtc() { return utc; }
        public void setUtc(String utc) { this.utc = utc; }
        public boolean isUseful() { return useful; }
        public void setUseful(boolean useful) { this.useful = useful; }
        public Double getLongitudeDecimal() { return longitudeDecimal; }
        public void setLongitudeDecimal(Double longitudeDecimal) { this.longitudeDecimal = longitudeDecimal; }
        public Double getLatitudeDecimal() { return latitudeDecimal; }
        public void setLatitudeDecimal(Double latitudeDecimal) { this.latitudeDecimal = latitudeDecimal; }
    }

    /**
     * LBS定位信息
     */
    public static class LbsLocation {
        private Double longitude;
        private Double latitude;
        private boolean useful;

        // Getters and Setters
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public boolean isUseful() { return useful; }
        public void setUseful(boolean useful) { this.useful = useful; }
    }

    /**
     * 位置信息
     */
    public static class LocationInfo {
        private String type;        // 定位类型：BDS/LBS
        private Double longitude;   // 十进制经度
        private Double latitude;    // 十进制纬度
        private Double accuracy;    // 精度（米）

        public LocationInfo() {}

        public LocationInfo(String type, Double longitude, Double latitude, Double accuracy) {
            this.type = type;
            this.longitude = longitude;
            this.latitude = latitude;
            this.accuracy = accuracy;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    }
}