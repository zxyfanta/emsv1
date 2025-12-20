package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.repository.RadiationDeviceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 辐射监测仪数据服务类
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RadiationDeviceDataService {

    private final RadiationDeviceDataRepository radiationDeviceDataRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 保存辐射监测数据
     */
    public RadiationDeviceData saveData(RadiationDeviceData data, Long companyId) {
        log.debug("Saving radiation data for device: {} in company: {}", data.getDeviceCode(), companyId);

        // 验证设备是否属于当前企业
        validateDeviceAccess(data.getDeviceCode(), companyId);

        // 设置记录时间
        if (data.getRecordTime() == null) {
            data.setRecordTime(LocalDateTime.now());
        }

        RadiationDeviceData savedData = radiationDeviceDataRepository.save(data);
        log.debug("Radiation data saved successfully with ID: {}", savedData.getId());

        return savedData;
    }

    /**
     * 批量保存辐射监测数据
     */
    public List<RadiationDeviceData> saveDataBatch(List<RadiationDeviceData> dataList, Long companyId) {
        log.debug("Saving batch radiation data: {} records for company: {}", dataList.size(), companyId);

        // 验证所有设备是否属于当前企业
        for (RadiationDeviceData data : dataList) {
            validateDeviceAccess(data.getDeviceCode(), companyId);
            if (data.getRecordTime() == null) {
                data.setRecordTime(LocalDateTime.now());
            }
        }

        List<RadiationDeviceData> savedData = radiationDeviceDataRepository.saveAll(dataList);
        log.info("Batch radiation data saved successfully: {} records", savedData.size());

        return savedData;
    }

    /**
     * 获取企业辐射数据列表
     */
    @Transactional(readOnly = true)
    public Page<RadiationDeviceData> getData(Long companyId, Pageable pageable) {
        log.debug("Getting radiation data for company: {} with pageable: {}", companyId, pageable);

        return radiationDeviceDataRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * 根据设备编码获取数据列表
     */
    @Transactional(readOnly = true)
    public Page<RadiationDeviceData> getDataByDeviceCode(String deviceCode, Pageable pageable) {
        log.debug("Getting radiation data for device: {} with pageable: {}", deviceCode, pageable);

        return radiationDeviceDataRepository.findByDeviceCodeOrderByRecordTimeDesc(deviceCode, pageable);
    }

    /**
     * 根据设备编码和时间范围获取数据
     */
    @Transactional(readOnly = true)
    public Page<RadiationDeviceData> getDataByDeviceCodeAndTimeRange(
            String deviceCode, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Getting radiation data for device: {} between {} and {}", deviceCode, startTime, endTime);

        return radiationDeviceDataRepository.findByDeviceCodeAndTimeRange(deviceCode, startTime, endTime, pageable);
    }

    /**
     * 根据时间范围获取企业数据
     */
    @Transactional(readOnly = true)
    public Page<RadiationDeviceData> getDataByTimeRange(
            Long companyId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Getting radiation data for company: {} between {} and {}", companyId, startTime, endTime);

        return radiationDeviceDataRepository.findByCompanyIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                companyId, startTime, endTime, pageable);
    }

    /**
     * 获取数据详情
     */
    @Transactional(readOnly = true)
    public RadiationDeviceData getDataDetail(Long id, Long companyId) {
        log.debug("Getting radiation data detail with ID: {} for company: {}", id, companyId);

        RadiationDeviceData data = radiationDeviceDataRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("辐射数据不存在"));

        // 验证数据是否属于当前企业
        validateDeviceAccess(data.getDeviceCode(), companyId);

        return data;
    }

    /**
     * 获取设备最新数据
     */
    @Transactional(readOnly = true)
    public RadiationDeviceData getLatestDataByDeviceCode(String deviceCode) {
        log.debug("Getting latest radiation data for device: {}", deviceCode);

        List<RadiationDeviceData> dataList = radiationDeviceDataRepository.findTopByDeviceCodeOrderByRecordTimeDesc(deviceCode);
        return dataList.isEmpty() ? null : dataList.get(0);
    }

    /**
     * 获取企业最新数据
     */
    @Transactional(readOnly = true)
    public RadiationDeviceData getLatestData(Long companyId) {
        log.debug("Getting latest radiation data for company: {}", companyId);

        List<RadiationDeviceData> dataList = radiationDeviceDataRepository.findTopByCompanyIdOrderByRecordTimeDesc(companyId);
        return dataList.isEmpty() ? null : dataList.get(0);
    }

    /**
     * 获取设备最新数据（带企业验证）
     */
    @Transactional(readOnly = true)
    public RadiationDeviceData getLatestDataByDeviceCode(String deviceCode, Long companyId) {
        log.debug("Getting latest radiation data for device: {} in company: {}", deviceCode, companyId);

        validateDeviceAccess(deviceCode, companyId);

        List<RadiationDeviceData> dataList = radiationDeviceDataRepository.findTopByDeviceCodeOrderByRecordTimeDesc(deviceCode);
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
        if (device.getDeviceType() != com.cdutetc.ems.entity.enums.DeviceType.RADIATION_MONITOR) {
            throw new IllegalArgumentException("设备 " + deviceCode + " 不是辐射监测设备");
        }
    }

    /**
     * 获取辐射数据统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(Long companyId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Getting radiation data statistics for company: {} between {} and {}", companyId, startTime, endTime);

        Map<String, Object> statistics = new HashMap<>();

        // 总记录数
        long totalRecords;
        if (startTime != null && endTime != null) {
            totalRecords = radiationDeviceDataRepository.countByCompanyIdAndRecordTimeBetween(companyId, startTime, endTime);
        } else {
            totalRecords = radiationDeviceDataRepository.countByCompanyId(companyId);
        }
        statistics.put("totalRecords", totalRecords);

        // 设备数量
        long deviceCount = radiationDeviceDataRepository.countDistinctDeviceCodeByCompanyId(companyId);
        statistics.put("deviceCount", deviceCount);

        // CPM 统计
        if (startTime != null && endTime != null) {
            Object[] cpmStats = radiationDeviceDataRepository.getCpmStatisticsByTimeRange(companyId, startTime, endTime);
            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
        } else {
            Object[] cpmStats = radiationDeviceDataRepository.getCpmStatistics(companyId);
            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
        }

        return statistics;
    }

    /**
     * 获取单个设备的统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsByDeviceCode(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Getting radiation data statistics for device: {} between {} and {}", deviceCode, startTime, endTime);

        Map<String, Object> statistics = new HashMap<>();

        // 总记录数
        long totalRecords;
        if (startTime != null && endTime != null) {
            totalRecords = radiationDeviceDataRepository.countByDeviceCodeAndRecordTimeBetween(deviceCode, startTime, endTime);
        } else {
            totalRecords = radiationDeviceDataRepository.countByDeviceCode(deviceCode);
        }
        statistics.put("totalRecords", totalRecords);

        // CPM 统计
        if (startTime != null && endTime != null) {
            Object[] cpmStats = radiationDeviceDataRepository.getCpmStatisticsByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);
            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
        } else {
            Object[] cpmStats = radiationDeviceDataRepository.getCpmStatisticsByDeviceCode(deviceCode);
            statistics.put("cpmStatistics", Map.of(
                    "average", cpmStats[0],
                    "minimum", cpmStats[1],
                    "maximum", cpmStats[2]
            ));
        }

        return statistics;
    }

    /**
     * 删除指定时间之前的数据（数据清理）
     */
    public long deleteDataBefore(LocalDateTime dateTime, Long companyId) {
        log.debug("Deleting radiation data before {} for company: {}", dateTime, companyId);

        long deletedCount = radiationDeviceDataRepository.deleteByRecordTimeBeforeAndCompanyId(dateTime, companyId);
        log.info("Deleted {} radiation data records before {} for company: {}", deletedCount, dateTime, companyId);

        return deletedCount;
    }
}