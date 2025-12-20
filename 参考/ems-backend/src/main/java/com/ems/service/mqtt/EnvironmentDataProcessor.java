package com.ems.service.mqtt;

import com.ems.entity.device.Device;
import com.ems.entity.EnvironmentDeviceStatus;
import com.ems.repository.EnvironmentDeviceStatusRepository;
import com.ems.service.device.DeviceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 环境监测设备数据处理器
 * 处理环境监测站的MQTT消息数据
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentDataProcessor implements DeviceDataProcessor {

    private final EnvironmentDeviceStatusRepository environmentDeviceStatusRepository;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String deviceType) {
        return "ENVIRONMENT".equals(deviceType);
    }

    @Override
    public void processMessage(String deviceId, String topic, String payload) {
        try {
            log.debug("🌡️ 处理环境监测设备数据: deviceId={}, topic={}", deviceId, topic);

            // 获取设备信息
            Device device = findDevice(deviceId);
            if (device == null) {
                log.warn("⚠️ 设备不存在: deviceId={}", deviceId);
                return;
            }

            // 解析JSON数据
            JsonNode rootNode = objectMapper.readTree(payload);

            // 创建环境设备状态记录
            EnvironmentDeviceStatus status = parseEnvironmentData(device, rootNode);

            // 保存数据
            environmentDeviceStatusRepository.save(status);

            log.debug("✅ 环境监测设备数据处理完成: deviceId={}, CPM={}, 温度={}°C, 湿度={}%, 电池={}V",
                     deviceId, status.getCpmValue(), status.getTemperature(),
                     status.getWetness(), status.getBatteryVoltageVolts());

        } catch (Exception e) {
            log.error("❌ 环境监测设备数据处理失败: deviceId={}, topic={}, payload={}", deviceId, topic, payload, e);

            // 创建错误记录
            Device errorDevice = findDevice(deviceId);
            if (errorDevice != null) {
                try {
                    EnvironmentDeviceStatus errorRecord = EnvironmentDeviceStatus.createErrorRecord(
                        errorDevice, e.getMessage()
                    );
                    environmentDeviceStatusRepository.save(errorRecord);
                } catch (Exception saveError) {
                    log.error("❌ 保存错误记录失败: deviceId={}", deviceId, saveError);
                }
            }
        }
    }

    /**
     * 解析环境监测设备数据
     * 严格按照数据格式：{"src": 1, "CPM": 4, "temperature": 10, "wetness": 95, "windspeed": 0.2, "total": 144.1, "battery": 11.9}
     */
    private EnvironmentDeviceStatus parseEnvironmentData(Device device, JsonNode rootNode) {
        EnvironmentDeviceStatus.EnvironmentDeviceStatusBuilder builder = EnvironmentDeviceStatus.builder()
                .device(device)
                .recordTime(LocalDateTime.now());

        // 解析数据源标识
        if (rootNode.has("src")) {
            builder.src(rootNode.get("src").asInt());
        }

        // 解析CPM辐射值
        if (rootNode.has("CPM")) {
            builder.cpmValue(rootNode.get("CPM").asInt());
        }

        // 解析温度（摄氏度）
        if (rootNode.has("temperature")) {
            builder.temperature(rootNode.get("temperature").asDouble());
        }

        // 解析湿度（百分比）
        if (rootNode.has("wetness")) {
            builder.wetness(rootNode.get("wetness").asDouble());
        }

        // 解析风速（米/秒）
        if (rootNode.has("windspeed")) {
            builder.windSpeed(rootNode.get("windspeed").asDouble());
        }

        // 解析综合环境指数
        if (rootNode.has("total")) {
            builder.totalEnvironmentIndex(BigDecimal.valueOf(rootNode.get("total").asDouble()));
        }

        // 解析电池电压（伏特）
        if (rootNode.has("battery")) {
            builder.batteryVoltage(BigDecimal.valueOf(rootNode.get("battery").asDouble()));
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
                        device.setDeviceName("环境监测站-" + deviceId);
                        device.setDeviceType(Device.DeviceType.ENVIRONMENT);
                        device.setStatus(Device.DeviceStatus.ONLINE);
                        return device;
                    });
        } catch (Exception e) {
            log.error("❌ 查找设备失败: deviceId={}", deviceId, e);
            return null;
        }
    }
}