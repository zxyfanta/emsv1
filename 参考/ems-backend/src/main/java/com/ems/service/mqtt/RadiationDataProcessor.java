package com.ems.service.mqtt;

import com.ems.entity.device.Device;
import com.ems.entity.RadiationDeviceStatus;
import com.ems.repository.RadiationDeviceStatusRepository;
import com.ems.service.device.DeviceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 辐射设备数据处理器
 * 处理辐射监测仪的MQTT消息数据
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RadiationDataProcessor implements DeviceDataProcessor {

    private final RadiationDeviceStatusRepository radiationDeviceStatusRepository;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    // 时间格式模式
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public boolean supports(String deviceType) {
        return "RADIATION".equals(deviceType);
    }

    @Override
    public void processMessage(String deviceId, String topic, String payload) {
        try {
            log.debug("📡 处理辐射设备数据: deviceId={}, topic={}", deviceId, topic);

            // 获取设备信息
            Device device = findDevice(deviceId);
            if (device == null) {
                log.warn("⚠️ 设备不存在: deviceId={}", deviceId);
                return;
            }

            // 解析JSON数据
            JsonNode rootNode = objectMapper.readTree(payload);

            // 创建辐射设备状态记录
            RadiationDeviceStatus status = parseRadiationData(device, rootNode);

            // 保存数据
            radiationDeviceStatusRepository.save(status);

            log.debug("✅ 辐射设备数据处理完成: deviceId={}, CPM={}, 电池={}mV",
                     deviceId, status.getCpmValue(), status.getBatteryVoltageMv());

        } catch (Exception e) {
            log.error("❌ 辐射设备数据处理失败: deviceId={}, topic={}, payload={}", deviceId, topic, payload, e);

            // 创建错误记录
            Device errorDevice = findDevice(deviceId);
            if (errorDevice != null) {
                try {
                    RadiationDeviceStatus errorRecord = RadiationDeviceStatus.createErrorRecord(
                        errorDevice, e.getMessage()
                    );
                    radiationDeviceStatusRepository.save(errorRecord);
                } catch (Exception saveError) {
                    log.error("❌ 保存错误记录失败: deviceId={}", deviceId, saveError);
                }
            }
        }
    }

    /**
     * 解析辐射设备数据
     */
    private RadiationDeviceStatus parseRadiationData(Device device, JsonNode rootNode) {
        RadiationDeviceStatus.RadiationDeviceStatusBuilder builder = RadiationDeviceStatus.builder()
                .device(device)
                .recordTime(LocalDateTime.now());

    
        // 解析GPS位置信息
        if (rootNode.has("BDS")) {
            JsonNode bdsNode = rootNode.get("BDS");
            if (bdsNode.has("longitude") && bdsNode.has("latitude")) {
                String longitude = bdsNode.get("longitude").asText();
                String latitude = bdsNode.get("latitude").asText();
                boolean useful = bdsNode.has("useful") && bdsNode.get("useful").asInt() == 1;

                builder.bdsLongitude(longitude)
                        .bdsLatitude(latitude)
                        .bdsUtc(bdsNode.has("UTC") ? bdsNode.get("UTC").asText() : null)
                        .bdsUseful(useful);
            }
        }

        if (rootNode.has("LBS")) {
            JsonNode lbsNode = rootNode.get("LBS");
            if (lbsNode.has("longitude") && lbsNode.has("latitude")) {
                builder.lbsLongitude(lbsNode.get("longitude").asDouble())
                        .lbsLatitude(lbsNode.get("latitude").asDouble())
                        .lbsUseful(lbsNode.has("useful") && lbsNode.get("useful").asInt() == 1);
            }
        }

        // 解析核心监测数据
        if (rootNode.has("CPM")) {
            builder.cpmValue(rootNode.get("CPM").asInt());
        }

        if (rootNode.has("Batvolt")) {
            Integer batteryVoltage = rootNode.get("Batvolt").asInt();
            builder.batteryVoltageMv(batteryVoltage);
        }

        if (rootNode.has("signal")) {
            builder.signalQuality(rootNode.get("signal").asInt());
        }

        if (rootNode.has("temperature")) {
            builder.deviceTemperature(rootNode.get("temperature").asDouble());
        }

        // 解析时间信息
        if (rootNode.has("time")) {
            String timeStr = rootNode.get("time").asText();
            builder.localTimeString(timeStr);
            try {
                LocalDateTime parsedTime = LocalDateTime.parse(timeStr, TIME_FORMATTER);
                builder.recordTime(parsedTime);
            } catch (Exception e) {
                log.warn("时间解析失败，使用当前时间: timeStr={}", timeStr);
            }
        }

        // 解析其他状态字段
        if (rootNode.has("trigger")) {
            // 可以将trigger信息存储到适当的字段
        }

        if (rootNode.has("multi")) {
            // 可以将multi信息存储到适当的字段
        }

        if (rootNode.has("msgtype")) {
            // 可以将msgtype信息存储到适当的字段
        }

        return builder.build();
    }

  
    /**
     * 查找设备信息
     * 集成DeviceService来查找真实设备数据
     */
    private Device findDevice(String deviceId) {
        try {
            return deviceService.findByDeviceId(deviceId)
                    .orElseGet(() -> {
                        log.warn("⚠️ 设备不存在，创建临时设备记录: deviceId={}", deviceId);
                        // 如果设备不存在，创建临时设备对象用于测试
                        Device device = new Device();
                        device.setDeviceId(deviceId);
                        device.setDeviceName("辐射监测仪-" + deviceId);
                        device.setDeviceType(Device.DeviceType.RADIATION);
                        device.setStatus(Device.DeviceStatus.ONLINE);
                        return device;
                    });
        } catch (Exception e) {
            log.error("❌ 查找设备失败: deviceId={}", deviceId, e);
            return null;
        }
    }
}