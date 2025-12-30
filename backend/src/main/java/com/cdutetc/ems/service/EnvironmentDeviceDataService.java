package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.repository.EnvironmentDeviceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 环境监测站数据服务类
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnvironmentDeviceDataService {

    private final EnvironmentDeviceDataRepository environmentDeviceDataRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 保存环境监测数据（不限企业）
     * 保存时清除统计缓存
     */
    @CacheEvict(value = "envStats", allEntries = true)
    public EnvironmentDeviceData save(EnvironmentDeviceData data) {
        log.debug("Saving environment data for device: {}", data.getDeviceCode());

        // 设置记录时间
        if (data.getRecordTime() == null) {
            data.setRecordTime(LocalDateTime.now());
        }

        EnvironmentDeviceData savedData = environmentDeviceDataRepository.save(data);
        log.debug("Environment data saved successfully with ID: {}", savedData.getId());

        return savedData;
    }

    /**
     * 保存环境监测数据
     */
    public EnvironmentDeviceData saveData(EnvironmentDeviceData data, Long companyId) {
        log.debug("Saving environment data for device: {} in company: {}", data.getDeviceCode(), companyId);

        // 验证设备是否属于当前企业
        validateDeviceAccess(data.getDeviceCode(), companyId);

        // 设置记录时间
        if (data.getRecordTime() == null) {
            data.setRecordTime(LocalDateTime.now());
        }

        EnvironmentDeviceData savedData = environmentDeviceDataRepository.save(data);
        log.debug("Environment data saved successfully with ID: {}", savedData.getId());

        return savedData;
    }

    /**
     * 批量保存环境监测数据
     */
    public List<EnvironmentDeviceData> saveDataBatch(List<EnvironmentDeviceData> dataList, Long companyId) {
        log.debug("Saving batch environment data: {} records for company: {}", dataList.size(), companyId);

        // 验证所有设备是否属于当前企业
        for (EnvironmentDeviceData data : dataList) {
            validateDeviceAccess(data.getDeviceCode(), companyId);
            if (data.getRecordTime() == null) {
                data.setRecordTime(LocalDateTime.now());
            }
        }

        List<EnvironmentDeviceData> savedData = environmentDeviceDataRepository.saveAll(dataList);
        log.info("Batch environment data saved successfully: {} records", savedData.size());

        return savedData;
    }

    /**
     * 获取企业环境数据列表
     */
    @Transactional(readOnly = true)
    public Page<EnvironmentDeviceData> getData(Long companyId, Pageable pageable) {
        log.debug("Getting environment data for company: {} with pageable: {}", companyId, pageable);

        return environmentDeviceDataRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * 根据设备编码获取数据列表
     */
    @Transactional(readOnly = true)
    public Page<EnvironmentDeviceData> getDataByDeviceCode(String deviceCode, Pageable pageable) {
        log.debug("Getting environment data for device: {} with pageable: {}", deviceCode, pageable);

        return environmentDeviceDataRepository.findByDeviceCodeOrderByRecordTimeDesc(deviceCode, pageable);
    }

    /**
     * 根据设备编码和时间范围获取数据
     */
    @Transactional(readOnly = true)
    public Page<EnvironmentDeviceData> getDataByDeviceCodeAndTimeRange(
            String deviceCode, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Getting environment data for device: {} between {} and {}", deviceCode, startTime, endTime);

        return environmentDeviceDataRepository.findByDeviceCodeAndRecordTimeBetweenOrderByRecordTimeDesc(
                deviceCode, startTime, endTime, pageable);
    }

    /**
     * 根据时间范围获取企业数据
     */
    @Transactional(readOnly = true)
    public Page<EnvironmentDeviceData> getDataByTimeRange(
            Long companyId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Getting environment data for company: {} between {} and {}", companyId, startTime, endTime);

        return environmentDeviceDataRepository.findByCompanyIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                companyId, startTime, endTime, pageable);
    }

    /**
     * 获取数据详情
     */
    @Transactional(readOnly = true)
    public EnvironmentDeviceData getDataDetail(Long id, Long companyId) {
        log.debug("Getting environment data detail with ID: {} for company: {}", id, companyId);

        EnvironmentDeviceData data = environmentDeviceDataRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("环境数据不存在"));

        // 验证数据是否属于当前企业
        validateDeviceAccess(data.getDeviceCode(), companyId);

        return data;
    }

    /**
     * 获取设备最新数据
     */
    @Transactional(readOnly = true)
    public EnvironmentDeviceData getLatestDataByDeviceCode(String deviceCode) {
        log.debug("Getting latest environment data for device: {}", deviceCode);

        List<EnvironmentDeviceData> dataList = environmentDeviceDataRepository.findTopByDeviceCodeOrderByRecordTimeDesc(deviceCode);
        return dataList.isEmpty() ? null : dataList.get(0);
    }

    /**
     * 获取企业最新数据
     */
    @Transactional(readOnly = true)
    public EnvironmentDeviceData getLatestData(Long companyId) {
        log.debug("Getting latest environment data for company: {}", companyId);

        List<EnvironmentDeviceData> dataList = environmentDeviceDataRepository.findTopByCompanyIdOrderByRecordTimeDesc(companyId);
        return dataList.isEmpty() ? null : dataList.get(0);
    }

    /**
     * 获取设备最新数据（带企业验证）
     */
    @Transactional(readOnly = true)
    public EnvironmentDeviceData getLatestDataByDeviceCode(String deviceCode, Long companyId) {
        log.debug("Getting latest environment data for device: {} in company: {}", deviceCode, companyId);

        validateDeviceAccess(deviceCode, companyId);

        List<EnvironmentDeviceData> dataList = environmentDeviceDataRepository.findTopByDeviceCodeOrderByRecordTimeDesc(deviceCode);
        return dataList.isEmpty() ? null : dataList.get(0);
    }

    /**
     * 验证设备访问权限
     */
    @Transactional(readOnly = true)
    public void validateDeviceAccess(String deviceCode, Long companyId) {
        log.debug("Validating device access: {} for company: {}", deviceCode, companyId);

        Device device = deviceRepository.findByDeviceCodeAndCompanyId(deviceCode, companyId)
                .orElseThrow(() -> new IllegalArgumentException("设备 " + deviceCode + " 不存在或不属于当前企业"));

        // 验证设备类型
        if (device.getDeviceType() != DeviceType.ENVIRONMENT_STATION) {
            throw new IllegalArgumentException("设备 " + deviceCode + " 不是环境监测设备");
        }
    }

    /**
     * 获取环境数据统计信息
     * 使用缓存减少重复查询
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "envStats",
               key = "#companyId + '-' + #startTime + '-' + #endTime")
    public Map<String, Object> getStatistics(Long companyId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Getting environment data statistics for company: {} between {} and {}", companyId, startTime, endTime);

        Map<String, Object> statistics = new HashMap<>();

        // 总记录数
        long totalRecords;
        if (startTime != null && endTime != null) {
            totalRecords = environmentDeviceDataRepository.countByCompanyIdAndRecordTimeBetween(companyId, startTime, endTime);
        } else {
            totalRecords = environmentDeviceDataRepository.countByCompanyId(companyId);
        }
        statistics.put("totalRecords", totalRecords);

        // 设备数量
        long deviceCount = environmentDeviceDataRepository.countDistinctDeviceCodeByCompanyId(companyId);
        statistics.put("deviceCount", deviceCount);

        // CPM 统计
        if (startTime != null && endTime != null) {
            Object[] cpmStats = environmentDeviceDataRepository.getCpmStatisticsByTimeRange(companyId, startTime, endTime);
            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
        } else {
            Object[] cpmStats = environmentDeviceDataRepository.getCpmStatistics(companyId);
            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
        }

        // 环境参数统计
        if (startTime != null && endTime != null) {
            Object[] tempStats = environmentDeviceDataRepository.getTemperatureStatisticsByTimeRange(companyId, startTime, endTime);
            Object[] humidityStats = environmentDeviceDataRepository.getHumidityStatisticsByTimeRange(companyId, startTime, endTime);
            Object[] windStats = environmentDeviceDataRepository.getWindSpeedStatisticsByTimeRange(companyId, startTime, endTime);
            Object[] batteryStats = environmentDeviceDataRepository.getBatteryStatisticsByTimeRange(companyId, startTime, endTime);

            statistics.put("temperatureStatistics", Map.of(
                    "average", tempStats[0],
                    "minimum", tempStats[1],
                    "maximum", tempStats[2]
            ));
            statistics.put("humidityStatistics", Map.of(
                    "average", humidityStats[0],
                    "minimum", humidityStats[1],
                    "maximum", humidityStats[2]
            ));
            statistics.put("windSpeedStatistics", Map.of(
                    "average", windStats[0],
                    "minimum", windStats[1],
                    "maximum", windStats[2]
            ));
            statistics.put("batteryStatistics", Map.of(
                    "average", batteryStats[0],
                    "minimum", batteryStats[1],
                    "maximum", batteryStats[2]
            ));
        } else {
            Object[] tempStats = environmentDeviceDataRepository.getTemperatureStatistics(companyId);
            Object[] humidityStats = environmentDeviceDataRepository.getHumidityStatistics(companyId);
            Object[] windStats = environmentDeviceDataRepository.getWindSpeedStatistics(companyId);
            Object[] batteryStats = environmentDeviceDataRepository.getBatteryStatistics(companyId);

            statistics.put("temperatureStatistics", Map.of(
                    "average", tempStats[0],
                    "minimum", tempStats[1],
                    "maximum", tempStats[2]
            ));
            statistics.put("humidityStatistics", Map.of(
                    "average", humidityStats[0],
                    "minimum", humidityStats[1],
                    "maximum", humidityStats[2]
            ));
            statistics.put("windSpeedStatistics", Map.of(
                    "average", windStats[0],
                    "minimum", windStats[1],
                    "maximum", windStats[2]
            ));
            statistics.put("batteryStatistics", Map.of(
                    "average", batteryStats[0],
                    "minimum", batteryStats[1],
                    "maximum", batteryStats[2]
            ));
        }

        return statistics;
    }

    /**
     * 获取单个设备的统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsByDeviceCode(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Getting environment data statistics for device: {} between {} and {}", deviceCode, startTime, endTime);

        Map<String, Object> statistics = new HashMap<>();

        // 总记录数
        long totalRecords;
        if (startTime != null && endTime != null) {
            totalRecords = environmentDeviceDataRepository.countByDeviceCodeAndRecordTimeBetween(deviceCode, startTime, endTime);
        } else {
            totalRecords = environmentDeviceDataRepository.countByDeviceCode(deviceCode);
        }
        statistics.put("totalRecords", totalRecords);

        // CPM 统计
        if (startTime != null && endTime != null) {
            Object[] cpmStats = environmentDeviceDataRepository.getCpmStatisticsByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);
            Object[] tempStats = environmentDeviceDataRepository.getTemperatureStatisticsByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);
            Object[] humidityStats = environmentDeviceDataRepository.getHumidityStatisticsByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);
            Object[] windStats = environmentDeviceDataRepository.getWindSpeedStatisticsByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);
            Object[] batteryStats = environmentDeviceDataRepository.getBatteryStatisticsByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);

            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
            statistics.put("temperatureStatistics", Map.of(
                    "average", tempStats[0],
                    "minimum", tempStats[1],
                    "maximum", tempStats[2]
            ));
            statistics.put("humidityStatistics", Map.of(
                    "average", humidityStats[0],
                    "minimum", humidityStats[1],
                    "maximum", humidityStats[2]
            ));
            statistics.put("windSpeedStatistics", Map.of(
                    "average", windStats[0],
                    "minimum", windStats[1],
                    "maximum", windStats[2]
            ));
            statistics.put("batteryStatistics", Map.of(
                    "average", batteryStats[0],
                    "minimum", batteryStats[1],
                    "maximum", batteryStats[2]
            ));
        } else {
            Object[] cpmStats = environmentDeviceDataRepository.getCpmStatisticsByDeviceCode(deviceCode);
            Object[] tempStats = environmentDeviceDataRepository.getTemperatureStatisticsByDeviceCode(deviceCode);
            Object[] humidityStats = environmentDeviceDataRepository.getHumidityStatisticsByDeviceCode(deviceCode);
            Object[] windStats = environmentDeviceDataRepository.getWindSpeedStatisticsByDeviceCode(deviceCode);
            Object[] batteryStats = environmentDeviceDataRepository.getBatteryStatisticsByDeviceCode(deviceCode);

            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
            statistics.put("temperatureStatistics", Map.of(
                    "average", tempStats[0],
                    "minimum", tempStats[1],
                    "maximum", tempStats[2]
            ));
            statistics.put("humidityStatistics", Map.of(
                    "average", humidityStats[0],
                    "minimum", humidityStats[1],
                    "maximum", humidityStats[2]
            ));
            statistics.put("windSpeedStatistics", Map.of(
                    "average", windStats[0],
                    "minimum", windStats[1],
                    "maximum", windStats[2]
            ));
            statistics.put("batteryStatistics", Map.of(
                    "average", batteryStats[0],
                    "minimum", batteryStats[1],
                    "maximum", batteryStats[2]
            ));
        }

        return statistics;
    }

    /**
     * 删除指定时间之前的数据（数据清理）
     */
    public long deleteDataBefore(LocalDateTime dateTime, Long companyId) {
        log.debug("Deleting environment data before {} for company: {}", dateTime, companyId);

        long deletedCount = environmentDeviceDataRepository.deleteByRecordTimeBeforeAndCompanyId(dateTime, companyId);
        log.info("Deleted {} environment data records before {} for company: {}", deletedCount, dateTime, companyId);

        return deletedCount;
    }
}