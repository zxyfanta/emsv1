package com.ems.service;

import com.ems.mqtt.MqttMessageListener.DeviceDataMessage;
import com.ems.entity.DeviceSensorData;
import com.ems.entity.device.Device;
import com.ems.repository.DeviceSensorDataRepository;
import com.ems.repository.device.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 设备数据服务
 * 处理设备数据的存储和业务逻辑
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceSensorDataRepository deviceSensorDataRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 保存设备数据
     *
     * @param deviceData 设备数据
     */
    @Transactional
    public void saveDeviceData(DeviceDataMessage deviceData) {
        try {
            log.info("开始保存设备数据: 设备ID={}, 时间={}, CPM={}",
                    deviceData.getDeviceId(), deviceData.getTime(), deviceData.getCpm());

            // 1. 获取设备信息以确定企业ID
            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceData.getDeviceId());
            if (deviceOpt.isEmpty()) {
                log.warn("设备不存在，跳过数据保存: 设备ID={}", deviceData.getDeviceId());
                return;
            }

            Device device = deviceOpt.get();
            Long enterpriseId = device.getEnterpriseId();

            // 2. 解析设备时间
            LocalDateTime deviceTime = parseDeviceTime(deviceData.getTime());

            // 3. 保存CPM数据（如果有）
            if (deviceData.getCpm() != null) {
                saveSensorData(deviceData.getDeviceId(), enterpriseId,
                             DeviceSensorData.MetricName.CPM.getCode(),
                             deviceData.getCpm().doubleValue(), "cpm", deviceTime);
            }

            // 4. 保存电池电压数据（如果有）
            if (deviceData.getBattery() != null && !deviceData.getBattery().isEmpty()) {
                try {
                    Double batteryVoltage = Double.parseDouble(deviceData.getBattery());
                    saveSensorData(deviceData.getDeviceId(), enterpriseId,
                                 DeviceSensorData.MetricName.BATVOLT.getCode(),
                                 batteryVoltage, "mV", deviceTime);
                } catch (NumberFormatException e) {
                    log.warn("电池电压数据格式错误，跳过保存: 设备ID={}, 电池值={}",
                            deviceData.getDeviceId(), deviceData.getBattery());
                }
            }

            log.info("设备数据保存完成: 设备ID={}, 企业ID={}, CPM={}, 电池={}mV",
                    deviceData.getDeviceId(), enterpriseId, deviceData.getCpm(), deviceData.getBattery());

            // 5. 记录位置信息（如果需要）
            if (deviceData.getLocation() != null) {
                log.info("位置信息记录 - 设备: {}, 经度: {}, 纬度: {}",
                        deviceData.getDeviceId(),
                        deviceData.getLocation().getLongitude(),
                        deviceData.getLocation().getLatitude());
            }

        } catch (Exception e) {
            log.error("保存设备数据失败: 设备ID={}, 错误={}", deviceData.getDeviceId(), e.getMessage(), e);
            throw new RuntimeException("保存设备数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存单个传感器数据
     *
     * @param deviceId 设备ID
     * @param enterpriseId 企业ID
     * @param metricName 指标名称
     * @param value 指标值
     * @param unit 单位
     * @param recordedAt 记录时间
     */
    private void saveSensorData(String deviceId, Long enterpriseId, String metricName,
                               Double value, String unit, LocalDateTime recordedAt) {
        try {
            DeviceSensorData sensorData = DeviceSensorData.builder()
                    .deviceId(deviceId)
                    .enterpriseId(enterpriseId)
                    .metricName(metricName)
                    .value(value)
                    .unit(unit)
                    .recordedAt(recordedAt)
                    .createdAt(LocalDateTime.now())
                    .build();

            // 数据验证
            if (!sensorData.isValid()) {
                log.warn("传感器数据验证失败，跳过保存: 设备ID={}, 指标={}, 值={}",
                        deviceId, metricName, value);
                return;
            }

            // 异常值检测
            if (sensorData.isAnomalous()) {
                log.warn("检测到异常传感器数据: 设备ID={}, 指标={}, 值={}",
                        deviceId, metricName, value);
                // 可以选择是否保存异常数据，这里选择保存但记录警告
            }

            deviceSensorDataRepository.save(sensorData);
            log.debug("传感器数据保存成功: 设备ID={}, 指标={}, 值={}",
                    deviceId, metricName, value);

        } catch (Exception e) {
            log.error("保存传感器数据失败: 设备ID={}, 指标={}, 错误={}",
                    deviceId, metricName, e.getMessage(), e);
            throw e; // 重新抛出异常，让上层处理
        }
    }

    /**
     * 解析设备时间
     *
     * @param timeStr 时间字符串
     * @return 解析后的时间，如果解析失败则返回当前时间
     */
    private LocalDateTime parseDeviceTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            log.warn("设备时间为空，使用当前时间");
            return LocalDateTime.now();
        }

        try {
            // 尝试多种时间格式解析
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(timeStr, formatter);
                } catch (Exception ignored) {
                    // 继续尝试下一个格式
                }
            }

            // 如果所有格式都失败，尝试时间戳转换
            try {
                long timestamp = Long.parseLong(timeStr);
                return LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.ofHours(8));
            } catch (Exception ignored) {
                // 忽略时间戳解析错误
            }

            log.warn("无法解析设备时间格式，使用当前时间: {}", timeStr);
            return LocalDateTime.now();

        } catch (Exception e) {
            log.warn("解析设备时间出错，使用当前时间: 时间={}, 错误={}", timeStr, e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * 获取设备最新传感器数据
     *
     * @param deviceId 设备ID
     * @param metricName 指标名称
     * @return 最新的传感器数据
     */
    public Optional<DeviceSensorData> getLatestSensorData(String deviceId, String metricName) {
        try {
            return deviceSensorDataRepository.findLatestByDeviceAndMetricLimitOne(deviceId, metricName);
        } catch (Exception e) {
            log.error("查询设备最新传感器数据失败: 设备ID={}, 指标={}, 错误={}",
                    deviceId, metricName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 获取设备历史数据
     *
     * @param deviceId 设备ID
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 历史数据列表
     */
    public List<DeviceSensorData> getSensorDataHistory(String deviceId, String metricName,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return deviceSensorDataRepository.findByDeviceIdAndMetricNameAndRecordedAtBetween(
                    deviceId, metricName, startTime, endTime);
        } catch (Exception e) {
            log.error("查询设备历史数据失败: 设备ID={}, 指标={}, 错误={}",
                    deviceId, metricName, e.getMessage(), e);
            return List.of();
        }
    }
}