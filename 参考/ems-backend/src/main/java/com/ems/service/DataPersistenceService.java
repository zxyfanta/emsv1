package com.ems.service;

import com.ems.entity.device.Device;
import com.ems.entity.DeviceStatusRecord;
import com.ems.entity.GpsRecord;
import com.ems.repository.DeviceStatusRecordRepository;
import com.ems.repository.GpsRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据持久化服务（简化版：MySQL + Redis）
 * 负责将MQTT接收的设备数据存储到MySQL数据库
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPersistenceService {

    private final GpsRecordRepository gpsRecordRepository;
    private final DeviceStatusRecordRepository deviceStatusRecordRepository;
    private final RedisCacheService redisCacheService;
    private final DeviceCacheService deviceCacheService;

    /**
     * 保存GPS轨迹数据
     */
    @Transactional
    public GpsRecord saveGpsRecord(GpsRecord gpsRecord) {
        try {
            // 保存到MySQL
            GpsRecord savedRecord = gpsRecordRepository.save(gpsRecord);
            log.debug("GPS轨迹数据保存成功: 设备={}, 经度={}, 纬度={}",
                     gpsRecord.getDevice().getDeviceId(),
                     gpsRecord.getPrimaryLongitude(),
                     gpsRecord.getPrimaryLatitude());

            // 更新Redis缓存中的设备状态
            updateDeviceLocationInRedis(gpsRecord);

            return savedRecord;

        } catch (Exception e) {
            log.error("GPS轨迹数据保存失败: 设备={}",
                     gpsRecord.getDevice().getDeviceId(), e);
            throw new RuntimeException("GPS数据保存失败", e);
        }
    }

    /**
     * 批量保存GPS轨迹数据
     */
    @Transactional
    public List<GpsRecord> batchSaveGpsRecords(List<GpsRecord> gpsRecords) {
        try {
            List<GpsRecord> savedRecords = gpsRecordRepository.saveAll(gpsRecords);
            log.debug("批量保存GPS轨迹数据成功: 数量={}", gpsRecords.size());

            // 批量更新Redis缓存
            gpsRecords.forEach(this::updateDeviceLocationInRedis);

            return savedRecords;

        } catch (Exception e) {
            log.error("批量保存GPS轨迹数据失败: 数量={}", gpsRecords.size(), e);
            throw new RuntimeException("批量GPS数据保存失败", e);
        }
    }

    /**
     * 保存设备状态数据
     */
    @Transactional
    public DeviceStatusRecord saveDeviceStatusRecord(DeviceStatusRecord statusRecord) {
        try {
            // 保存到MySQL
            DeviceStatusRecord savedRecord = deviceStatusRecordRepository.save(statusRecord);
            log.debug("设备状态数据保存成功: 设备={}, CPM={}, 电池={}mV",
                     statusRecord.getDevice().getDeviceId(),
                     statusRecord.getCpmValue(),
                     statusRecord.getBatteryVoltageMv());

            // 更新Redis缓存中的设备状态
            updateDeviceStatusInRedis(statusRecord);

            // 保存5分钟实时数据到Redis
            saveRealtimeDataToRedis(statusRecord);

            return savedRecord;

        } catch (Exception e) {
            log.error("设备状态数据保存失败: 设备={}",
                     statusRecord.getDevice().getDeviceId(), e);
            throw new RuntimeException("设备状态数据保存失败", e);
        }
    }

    /**
     * 批量保存设备状态数据
     */
    @Transactional
    public List<DeviceStatusRecord> batchSaveDeviceStatusRecords(List<DeviceStatusRecord> statusRecords) {
        try {
            List<DeviceStatusRecord> savedRecords = deviceStatusRecordRepository.saveAll(statusRecords);
            log.debug("批量保存设备状态数据成功: 数量={}", statusRecords.size());

            // 批量更新Redis缓存
            statusRecords.forEach(this::updateDeviceStatusInRedis);
            statusRecords.forEach(this::saveRealtimeDataToRedis);

            return savedRecords;

        } catch (Exception e) {
            log.error("批量保存设备状态数据失败: 数量={}", statusRecords.size(), e);
            throw new RuntimeException("批量设备状态数据保存失败", e);
        }
    }

    /**
     * 保存完整的GPS数据包（包含位置和状态信息）
     */
    @Transactional
    public void saveCompleteGpsData(Device device, String deviceId,
                                   Double bdsLng, Double bdsLat, String utcTime, Boolean bdsUseful,
                                   Double lbsLng, Double lbsLat, Boolean lbsUseful,
                                   Integer cpmValue, Integer batteryVoltageMv,
                                   Integer triggerType, Integer transmissionWay,
                                   Integer multiFlag, Integer messageType, Integer sourceFlag,
                                   String localTimeString) {
        try {
            LocalDateTime recordTime = LocalDateTime.now();

            // 1. 保存GPS轨迹记录
            GpsRecord gpsRecord = GpsRecord.createFromMqttData(
                    device, deviceId, bdsLng, bdsLat, utcTime, bdsUseful,
                    lbsLng, lbsLat, lbsUseful, triggerType, transmissionWay,
                    multiFlag, messageType, sourceFlag, localTimeString);
            gpsRecord.setRecordTime(recordTime);
            saveGpsRecord(gpsRecord);

            // 2. 保存设备状态记录（如果有CPM或电池数据）
            if (cpmValue != null || batteryVoltageMv != null) {
                DeviceStatusRecord statusRecord = DeviceStatusRecord.createFromGpsData(
                        device, cpmValue, batteryVoltageMv, localTimeString);
                statusRecord.setRecordTime(recordTime);
                saveDeviceStatusRecord(statusRecord);
            }

            // 3. 更新设备的最后在线时间
            device.setLastOnlineAt(recordTime);
            device.setStatus(Device.DeviceStatus.ONLINE);

            log.info("完整GPS数据保存成功: 设备={}, CPM={}, 电池={}mV, 位置=({},{})",
                     deviceId, cpmValue, batteryVoltageMv,
                     gpsRecord.getPrimaryLongitude(), gpsRecord.getPrimaryLatitude());

        } catch (Exception e) {
            log.error("完整GPS数据保存失败: 设备={}", deviceId, e);
            // 创建错误记录
            DeviceStatusRecord errorRecord = DeviceStatusRecord.createErrorRecord(
                    device, "GPS数据处理失败: " + e.getMessage(), "GPS");
            deviceStatusRecordRepository.save(errorRecord);
        }
    }

    /**
     * 更新设备位置信息到Redis缓存
     */
    private void updateDeviceLocationInRedis(GpsRecord gpsRecord) {
        try {
            String deviceId = gpsRecord.getDevice().getDeviceId();

            // 更新设备状态缓存中的位置信息
            redisCacheService.updateDeviceLocationStatus(
                    deviceId,
                    gpsRecord.getPrimaryLongitude(),
                    gpsRecord.getPrimaryLatitude(),
                    gpsRecord.getPrimaryType(),
                    gpsRecord.getRecordTime()
            );

            // 同步更新设备信息缓存
            deviceCacheService.refreshDeviceCache(deviceId);

            log.debug("设备位置状态缓存更新成功: 设备={}", deviceId);

        } catch (Exception e) {
            log.warn("更新设备位置缓存失败: 设备={}", gpsRecord.getDevice().getDeviceId(), e);
        }
    }

    /**
     * 更新设备状态到Redis缓存
     */
    private void updateDeviceStatusInRedis(DeviceStatusRecord statusRecord) {
        try {
            String deviceId = statusRecord.getDevice().getDeviceId();

            // 更新设备基本信息
            redisCacheService.cacheDeviceStatus(deviceId, "ONLINE");

            // 同步更新设备信息缓存
            deviceCacheService.refreshDeviceCache(deviceId);

            // 缓存实时状态数据
            if (statusRecord.getCpmValue() != null) {
                redisCacheService.cacheRealTimeData(
                        deviceId, "CPM", statusRecord.getCpmValue(), statusRecord.getRecordTime());
            }

            if (statusRecord.getBatteryVoltageMv() != null) {
                Double voltageVolts = statusRecord.getBatteryVoltageMv() / 1000.0;
                redisCacheService.cacheRealTimeData(
                        deviceId, "BatteryVoltage", voltageVolts, statusRecord.getRecordTime());

                // 缓存电池电量百分比
                Integer batteryLevel = statusRecord.getEstimatedBatteryLevel();
                if (batteryLevel != null) {
                    redisCacheService.cacheRealTimeData(
                            deviceId, "BatteryLevel", batteryLevel, statusRecord.getRecordTime());
                }
            }

            if (statusRecord.getSignalQuality() != null) {
                redisCacheService.cacheRealTimeData(
                        deviceId, "SignalQuality", statusRecord.getSignalQuality(), statusRecord.getRecordTime());
            }

            log.debug("设备状态缓存更新成功: 设备={}", deviceId);

        } catch (Exception e) {
            log.warn("更新设备状态缓存失败: 设备={}", statusRecord.getDevice().getDeviceId(), e);
        }
    }

    /**
     * 保存5分钟实时数据到Redis
     */
    private void saveRealtimeDataToRedis(DeviceStatusRecord statusRecord) {
        try {
            String deviceId = statusRecord.getDevice().getDeviceId();

            // 保存CPM实时数据（用于图表展示）
            if (statusRecord.getCpmValue() != null) {
                redisCacheService.saveRealtimeCpmData(deviceId, statusRecord.getCpmValue(), statusRecord.getRecordTime());
            }

            // 保存电池电压实时数据
            if (statusRecord.getBatteryVoltageMv() != null) {
                Double voltageVolts = statusRecord.getBatteryVoltageMv() / 1000.0;
                redisCacheService.saveRealtimeBatteryData(deviceId, voltageVolts, statusRecord.getRecordTime());
            }

            log.debug("实时数据保存到Redis成功: 设备={}", deviceId);

        } catch (Exception e) {
            log.warn("保存实时数据到Redis失败: 设备={}", statusRecord.getDevice().getDeviceId(), e);
        }
    }

    /**
     * 查询设备的GPS轨迹历史
     */
    public List<GpsRecord> getDeviceGpsHistory(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<GpsRecord> records = gpsRecordRepository.findByDeviceIdAndTimeRange(deviceId, startTime, endTime);
            log.debug("查询设备GPS轨迹: 设备={}, 记录数={}", deviceId, records.size());
            return records;

        } catch (Exception e) {
            log.error("查询设备GPS轨迹失败: 设备={}", deviceId, e);
            return List.of();
        }
    }

    /**
     * 查询设备状态历史
     */
    public List<DeviceStatusRecord> getDeviceStatusHistory(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<DeviceStatusRecord> records = deviceStatusRecordRepository.findByDeviceIdAndTimeRange(deviceId, startTime, endTime);
            log.debug("查询设备状态历史: 设备={}, 记录数={}", deviceId, records.size());
            return records;

        } catch (Exception e) {
            log.error("查询设备状态历史失败: 设备={}", deviceId, e);
            return List.of();
        }
    }

    /**
     * 查询设备最新位置
     */
    public GpsRecord getDeviceLatestLocation(String deviceId) {
        try {
            List<GpsRecord> records = gpsRecordRepository.findLatestByDeviceId(deviceId,
                    org.springframework.data.domain.PageRequest.of(0, 1));
            GpsRecord record = records.isEmpty() ? null : records.get(0);
            log.debug("查询设备最新位置: 设备={}, 位置=({},{})",
                     deviceId,
                     record != null ? record.getPrimaryLongitude() : null,
                     record != null ? record.getPrimaryLatitude() : null);
            return record;

        } catch (Exception e) {
            log.error("查询设备最新位置失败: 设备={}", deviceId, e);
            return null;
        }
    }

    /**
     * 数据清理：删除指定时间之前的历史数据
     */
    @Transactional
    public void cleanOldData(LocalDateTime thresholdDate) {
        try {
            // 删除旧的GPS记录
            gpsRecordRepository.deleteByRecordTimeBefore(thresholdDate);

            // 删除旧的状态记录
            deviceStatusRecordRepository.deleteByRecordTimeBefore(thresholdDate);

            log.info("历史数据清理完成: 删除{}之前的数据", thresholdDate);

        } catch (Exception e) {
            log.error("历史数据清理失败: 阈值日期={}", thresholdDate, e);
            throw new RuntimeException("数据清理失败", e);
        }
    }

    /**
     * 统计数据库中的记录数量
     */
    public void getDatabaseStatistics() {
        try {
            long gpsCount = gpsRecordRepository.count();
            long statusCount = deviceStatusRecordRepository.count();

            log.info("数据库统计: GPS记录={}, 状态记录={}", gpsCount, statusCount);

        } catch (Exception e) {
            log.error("获取数据库统计失败", e);
        }
    }
}