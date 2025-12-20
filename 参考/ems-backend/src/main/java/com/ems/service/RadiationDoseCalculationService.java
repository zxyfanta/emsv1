package com.ems.service;

import com.ems.exception.BusinessException;
import com.ems.exception.ValidationException;
import com.ems.entity.RadiationDeviceStatus;
import com.ems.entity.RadiationDoseRecord;
import com.ems.entity.device.Device;
import com.ems.exception.ErrorCode;
import com.ems.repository.RadiationDeviceStatusRepository;
import com.ems.repository.RadiationDoseRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 辐射剂量计算服务
 * 提供辐射剂量和剂量率的计算功能
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RadiationDoseCalculationService {

    private final RadiationDeviceStatusRepository radiationDeviceStatusRepository;
    private final RadiationDoseRecordRepository radiationDoseRecordRepository;

    // 转换因子：CPM 转 μSv/h
    // 这个因子需要根据具体的辐射探测器类型进行校准
    private static final BigDecimal CPM_TO_USV_PER_HOUR = new BigDecimal("0.0083");

    // 1小时 = 60分钟
    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal("60");

    // 1年 = 365天 = 8760小时
    private static final BigDecimal HOURS_PER_YEAR = new BigDecimal("8760");

    /**
     * 计算指定时间窗口内的剂量
     *
     * @param deviceId        设备ID
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @param calculationType 计算类型
     * @param correctionFactor 校正因子
     * @return 剂量记录
     */
    @Transactional
    public RadiationDoseRecord calculateDoseInTimeWindow(String deviceId,
                                                         LocalDateTime startTime,
                                                         LocalDateTime endTime,
                                                         RadiationDoseRecord.CalculationType calculationType,
                                                         BigDecimal correctionFactor) {
        validateTimeWindow(startTime, endTime);

        // 获取设备信息
        Device device = findDeviceById(deviceId);

        // 获取时间窗口内的辐射数据
        List<RadiationDeviceStatus> radiationData = radiationDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (radiationData.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                    "指定时间窗口内无辐射数据");
        }

        // 计算剂量
        RadiationDoseRecord doseRecord = calculateDoseFromData(device, radiationData, calculationType, correctionFactor);

        // 保存剂量记录
        RadiationDoseRecord savedRecord = radiationDoseRecordRepository.save(doseRecord);

        log.info("剂量计算完成: device={}, type={}, cumulativeDose={} μSv",
                deviceId, calculationType.getDescription(), savedRecord.getCumulativeDose());

        return savedRecord;
    }

    /**
     * 计算小时剂量
     *
     * @param deviceId 设备ID
     * @param hourTime 小时时间
     * @return 小时剂量记录
     */
    @Transactional
    public RadiationDoseRecord calculateHourlyDose(String deviceId, LocalDateTime hourTime) {
        LocalDateTime startTime = hourTime.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);

        return calculateDoseInTimeWindow(deviceId, startTime, endTime,
                RadiationDoseRecord.CalculationType.HOURLY, BigDecimal.ONE);
    }

    /**
     * 计算日剂量
     *
     * @param deviceId 设备ID
     * @param date     日期
     * @return 日剂量记录
     */
    @Transactional
    public RadiationDoseRecord calculateDailyDose(String deviceId, LocalDateTime date) {
        LocalDateTime startTime = date.toLocalDate().atStartOfDay();
        LocalDateTime endTime = startTime.plusDays(1);

        return calculateDoseInTimeWindow(deviceId, startTime, endTime,
                RadiationDoseRecord.CalculationType.DAILY, BigDecimal.ONE);
    }

    /**
     * 计算累积剂量
     *
     * @param deviceId 设备ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 累积剂量记录
     */
    @Transactional
    public RadiationDoseRecord calculateCumulativeDose(String deviceId, LocalDateTime startDate, LocalDateTime endDate) {
        validateTimeWindow(startDate, endDate);

        Device device = findDeviceById(deviceId);

        // 获取累积剂量的历史记录
        List<RadiationDoseRecord> existingRecords = radiationDoseRecordRepository
                .findByDeviceAndCalculationTypeOrderByRecordTimeDesc(
                        device, RadiationDoseRecord.CalculationType.CUMULATIVE);

        BigDecimal previousCumulativeDose = BigDecimal.ZERO;
        if (!existingRecords.isEmpty()) {
            RadiationDoseRecord lastRecord = existingRecords.get(0);
            previousCumulativeDose = lastRecord.getCumulativeDose() != null ?
                    lastRecord.getCumulativeDose() : BigDecimal.ZERO;
        }

        // 获取时间窗口内的辐射数据
        List<RadiationDeviceStatus> radiationData = radiationDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startDate, endDate);

        if (radiationData.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                    "指定时间窗口内无辐射数据");
        }

        // 计算新增剂量
        BigDecimal newDose = calculateDoseFromCpmData(radiationData);

        // 总累积剂量
        BigDecimal totalCumulativeDose = previousCumulativeDose.add(newDose);

        // 创建累积剂量记录
        RadiationDoseRecord cumulativeRecord = RadiationDoseRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .calculationType(RadiationDoseRecord.CalculationType.CUMULATIVE)
                .windowStartTime(startDate)
                .windowEndTime(endDate)
                .cumulativeDose(totalCumulativeDose)
                .dataPointsCount(radiationData.size())
                .radiationType(RadiationDoseRecord.RadiationType.GAMMA) // 默认伽马射线
                .correctionFactor(BigDecimal.ONE)
                .thresholdExceeded(checkThresholdExceeded(totalCumulativeDose))
                .build();

        // 更新剂量当量
        cumulativeRecord.updateDoseEquivalent();

        RadiationDoseRecord savedRecord = radiationDoseRecordRepository.save(cumulativeRecord);

        log.info("累积剂量计算完成: device={}, totalDose={} μSv, newDose={} μSv",
                deviceId, totalCumulativeDose, newDose);

        return savedRecord;
    }

    /**
     * 计算实时剂量率
     *
     * @param deviceId 设备ID
     * @return 剂量率记录
     */
    @Transactional
    public RadiationDoseRecord calculateRealtimeDoseRate(String deviceId) {
        Device device = findDeviceById(deviceId);

        // 获取最近5分钟的数据用于计算实时剂量率
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5);

        List<RadiationDeviceStatus> recentData = radiationDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTimeDesc(deviceId, startTime, endTime);

        if (recentData.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                    "最近5分钟内无辐射数据");
        }

        // 计算实时剂量率
        BigDecimal averageCpm = recentData.stream()
                .filter(data -> data.getCpmValue() != null)
                .mapToInt(RadiationDeviceStatus::getCpmValue)
                .average()
                .orElse(0.0);

        BigDecimal doseRate = BigDecimal.valueOf(averageCpm)
                .multiply(CPM_TO_USV_PER_HOUR)
                .setScale(4, RoundingMode.HALF_UP);

        RadiationDoseRecord realtimeRecord = RadiationDoseRecord.builder()
                .device(device)
                .recordTime(endTime)
                .calculationType(RadiationDoseRecord.CalculationType.REALTIME)
                .windowStartTime(startTime)
                .windowEndTime(endTime)
                .averageCpm(BigDecimal.valueOf(averageCpm))
                .doseRate(doseRate)
                .dataPointsCount(recentData.size())
                .radiationType(RadiationDoseRecord.RadiationType.GAMMA)
                .correctionFactor(BigDecimal.ONE)
                .thresholdExceeded(checkDoseRateThresholdExceeded(doseRate))
                .build();

        RadiationDoseRecord savedRecord = radiationDoseRecordRepository.save(realtimeRecord);

        log.info("实时剂量率计算完成: device={}, doseRate={} μSv/h, avgCpm={}",
                deviceId, doseRate, averageCpm);

        return savedRecord;
    }

    /**
     * 获取设备的剂量统计信息
     *
     * @param deviceId 设备ID
     * @param days     统计天数
     * @return 统计信息
     */
    public Map<String, Object> getDoseStatistics(String deviceId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        LocalDateTime endTime = LocalDateTime.now();

        List<RadiationDoseRecord> records = radiationDoseRecordRepository
                .findByDeviceAndRecordTimeBetweenOrderByRecordTimeDesc(
                        findDeviceById(deviceId), startTime, endTime);

        if (records.isEmpty()) {
            return Map.of("message", "指定时间范围内无剂量记录");
        }

        // 统计各种计算类型的记录
        Map<RadiationDoseRecord.CalculationType, List<RadiationDoseRecord>> recordsByType = records.stream()
                .collect(Collectors.groupingBy(RadiationDoseRecord::getCalculationType));

        BigDecimal maxCumulativeDose = records.stream()
                .filter(r -> r.getCumulativeDose() != null)
                .map(RadiationDoseRecord::getCumulativeDose)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxDoseRate = records.stream()
                .filter(r -> r.getDoseRate() != null)
                .map(RadiationDoseRecord::getDoseRate)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        long thresholdExceededCount = records.stream()
                .filter(RadiationDoseRecord::isThresholdExceeded)
                .count();

        return Map.of(
                "totalRecords", records.size(),
                "recordsByType", recordsByType.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().getDescription(),
                                entry -> entry.getValue().size()
                        )),
                "maxCumulativeDose", maxCumulativeDose,
                "maxDoseRate", maxDoseRate,
                "thresholdExceededCount", thresholdExceededCount,
                "statisticsPeriod", Map.of("startTime", startTime, "endTime", endTime)
        );
    }

    /**
     * 批量计算小时剂量
     *
     * @param deviceId 设备ID
     * @param hours    计算的小时数
     * @return 计算结果数量
     */
    @Transactional
    public int batchCalculateHourlyDose(String deviceId, int hours) {
        int calculatedCount = 0;
        LocalDateTime currentHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        for (int i = 0; i < hours; i++) {
            LocalDateTime hourTime = currentHour.minusHours(i);

            try {
                calculateHourlyDose(deviceId, hourTime);
                calculatedCount++;
            } catch (Exception e) {
                log.warn("计算{}小时剂量失败: {}", hourTime, e.getMessage());
            }
        }

        log.info("批量计算小时剂量完成: device={}, calculated={}", deviceId, calculatedCount);
        return calculatedCount;
    }

    /**
     * 从辐射数据计算剂量
     */
    private RadiationDoseRecord calculateDoseFromData(Device device,
                                                      List<RadiationDeviceStatus> radiationData,
                                                      RadiationDoseRecord.CalculationType calculationType,
                                                      BigDecimal correctionFactor) {
        // 计算统计信息
        List<Integer> cpmValues = radiationData.stream()
                .filter(data -> data.getCpmValue() != null)
                .map(RadiationDeviceStatus::getCpmValue)
                .collect(Collectors.toList());

        if (cpmValues.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                    "无有效的CPM数据");
        }

        double averageCpm = cpmValues.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int maxCpm = cpmValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minCpm = cpmValues.stream().mapToInt(Integer::intValue).min().orElse(0);

        // 计算累积剂量
        BigDecimal cumulativeDose = calculateDoseFromCpmData(radiationData);

        // 计算剂量率
        BigDecimal doseRate = BigDecimal.valueOf(averageCpm)
                .multiply(CPM_TO_USV_PER_HOUR)
                .setScale(4, RoundingMode.HALF_UP);

        LocalDateTime windowStartTime = radiationData.get(0).getRecordTime();
        LocalDateTime windowEndTime = radiationData.get(radiationData.size() - 1).getRecordTime();

        // 检查阈值
        boolean thresholdExceeded = checkThresholdExceeded(cumulativeDose);

        RadiationDoseRecord doseRecord = RadiationDoseRecord.builder()
                .device(device)
                .recordTime(LocalDateTime.now())
                .calculationType(calculationType)
                .windowStartTime(windowStartTime)
                .windowEndTime(windowEndTime)
                .averageCpm(BigDecimal.valueOf(averageCpm))
                .maxCpm(BigDecimal.valueOf(maxCpm))
                .minCpm(BigDecimal.valueOf(minCpm))
                .cumulativeDose(cumulativeDose)
                .doseRate(doseRate)
                .dataPointsCount(radiationData.size())
                .correctionFactor(correctionFactor != null ? correctionFactor : BigDecimal.ONE)
                .radiationType(RadiationDoseRecord.RadiationType.GAMMA)
                .thresholdExceeded(thresholdExceeded)
                .build();

        // 更新剂量当量
        doseRecord.updateDoseEquivalent();

        return doseRecord;
    }

    /**
     * 从CPM数据计算剂量
     */
    private BigDecimal calculateDoseFromCpmData(List<RadiationDeviceStatus> radiationData) {
        BigDecimal totalCpm = radiationData.stream()
                .filter(data -> data.getCpmValue() != null)
                .map(data -> BigDecimal.valueOf(data.getCpmValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 剂量 = 总CPM × 转换因子 × 时间窗口(小时)
        BigDecimal timeWindowHours = BigDecimal.valueOf(radiationData.size())
                .divide(MINUTES_PER_HOUR, 6, RoundingMode.HALF_UP);

        return totalCpm.multiply(CPM_TO_USV_PER_HOUR)
                .multiply(timeWindowHours)
                .setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 检查累积剂量阈值
     */
    private boolean checkThresholdExceeded(BigDecimal cumulativeDose) {
        // 设置阈值标准（可根据实际需求调整）
        BigDecimal warningThreshold = new BigDecimal("1.0"); // 1 μSv
        BigDecimal dangerThreshold = new BigDecimal("10.0"); // 10 μSv

        if (cumulativeDose.compareTo(dangerThreshold) >= 0) {
            return true;
        }

        return cumulativeDose.compareTo(warningThreshold) >= 0;
    }

    /**
     * 检查剂量率阈值
     */
    private boolean checkDoseRateThresholdExceeded(BigDecimal doseRate) {
        // 剂量率阈值标准（μSv/h）
        BigDecimal warningThreshold = new BigDecimal("0.5"); // 0.5 μSv/h
        BigDecimal dangerThreshold = new BigDecimal("2.5"); // 2.5 μSv/h

        return doseRate.compareTo(dangerThreshold) >= 0 || doseRate.compareTo(warningThreshold) >= 0;
    }

    /**
     * 验证时间窗口
     */
    private void validateTimeWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new ValidationException("时间窗口不能为空");
        }

        if (startTime.isAfter(endTime)) {
            throw new ValidationException("开始时间不能晚于结束时间");
        }

        if (startTime.isAfter(LocalDateTime.now())) {
            throw new ValidationException("开始时间不能晚于当前时间");
        }

        // 限制时间窗口最大为1年
        if (startTime.isBefore(LocalDateTime.now().minusYears(1))) {
            throw new ValidationException("时间窗口不能超过1年");
        }
    }

    /**
     * 根据设备ID查找设备
     */
    private Device findDeviceById(String deviceId) {
        // 这里应该调用DeviceService，简化实现
        Device device = new Device();
        device.setDeviceId(deviceId);
        return device;
    }
}