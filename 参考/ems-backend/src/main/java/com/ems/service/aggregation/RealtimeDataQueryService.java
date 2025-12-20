package com.ems.service.aggregation;

import com.ems.entity.DeviceStatusRecord;
import com.ems.repository.DeviceStatusRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实时数据查询服务
 * 直接查询device_status_records表，根据时间跨度进行智能采样
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimeDataQueryService {

    private final DeviceStatusRecordRepository deviceStatusRecordRepository;

    // 数据采样策略配置
    private static final int MINUTES_30_THRESHOLD = 1;      // 1天内：30分钟采样
    private static final int HOURLY_THRESHOLD = 7;          // 7天内：1小时采样
    private static final int SIX_HOURLY_THRESHOLD = 30;     // 30天内：6小时采样
    private static final int DAILY_THRESHOLD = 365;        // 365天内：24小时采样

    /**
     * 智能查询实时数据并采样
     *
     * @param deviceId 设备ID
     * @param metricName 指标名称 (CPM, Batvolt)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 图表数据点列表
     */
    public List<ChartDataRoutingService.ChartDataPoint> queryRealtimeData(
            String deviceId, String metricName, LocalDateTime startTime, LocalDateTime endTime) {

        long daysBetween = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate());

        log.debug("📊 查询实时数据: 设备={}, 指标={}, 时间跨度={}天", deviceId, metricName, daysBetween);

        // 查询原始数据
        List<DeviceStatusRecord> rawData = queryRawData(deviceId, metricName, startTime, endTime);

        if (rawData.isEmpty()) {
            log.debug("📋 没有找到数据: 设备={}, 指标={}", deviceId, metricName);
            return new ArrayList<>();
        }

        // 根据时间跨度进行采样
        List<ChartDataRoutingService.ChartDataPoint> sampledData = performSampling(rawData, daysBetween, metricName);

        log.debug("✅ 查询完成: 原始数据点={}, 采样后数据点={}", rawData.size(), sampledData.size());

        return sampledData;
    }

    /**
     * 查询原始数据
     */
    private List<DeviceStatusRecord> queryRawData(String deviceId, String metricName,
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        // 按时间倒序查询，获取最新数据
        return deviceStatusRecordRepository
            .findByDeviceIdAndTimeRange(deviceId, startTime, endTime);
    }

    /**
     * 根据时间跨度进行数据采样
     */
    private List<ChartDataRoutingService.ChartDataPoint> performSampling(
            List<DeviceStatusRecord> rawData, long daysBetween, String metricName) {

        if (daysBetween <= MINUTES_30_THRESHOLD) {
            // 1天内：30分钟采样
            return sampleByInterval(rawData, 30, metricName);
        } else if (daysBetween <= HOURLY_THRESHOLD) {
            // 7天内：1小时采样
            return sampleByInterval(rawData, 60, metricName);
        } else if (daysBetween <= SIX_HOURLY_THRESHOLD) {
            // 30天内：6小时采样
            return sampleByInterval(rawData, 360, metricName);
        } else if (daysBetween <= DAILY_THRESHOLD) {
            // 365天内：24小时采样
            return sampleByInterval(rawData, 1440, metricName);
        } else {
            // 超过365天：7天采样
            return sampleByInterval(rawData, 10080, metricName);
        }
    }

    /**
     * 按指定时间间隔采样数据
     */
    private List<ChartDataRoutingService.ChartDataPoint> sampleByInterval(
            List<DeviceStatusRecord> rawData, int intervalMinutes, String metricName) {

        if (rawData.isEmpty()) {
            return new ArrayList<>();
        }

        // 按时间正序排列
        rawData.sort(Comparator.comparing(DeviceStatusRecord::getRecordTime));

        List<ChartDataRoutingService.ChartDataPoint> result = new ArrayList<>();
        LocalDateTime currentIntervalStart = rawData.get(0).getRecordTime();
        LocalDateTime intervalEnd = currentIntervalStart.plusMinutes(intervalMinutes);

        List<Double> valuesInInterval = new ArrayList<>();

        for (DeviceStatusRecord record : rawData) {
            LocalDateTime recordTime = record.getRecordTime();

            // 如果记录时间超出当前间隔，创建一个数据点
            if (recordTime.isAfter(intervalEnd)) {
                if (!valuesInInterval.isEmpty()) {
                    double avgValue = valuesInInterval.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    result.add(new ChartDataRoutingService.ChartDataPoint(
                        currentIntervalStart, avgValue, valuesInInterval.size()));
                }

                // 开始下一个间隔
                currentIntervalStart = recordTime;
                intervalEnd = currentIntervalStart.plusMinutes(intervalMinutes);
                valuesInInterval.clear();
            }

            // 收集当前间隔内的值
            Double value = extractValue(record, metricName);
            if (value != null) {
                valuesInInterval.add(value);
            }
        }

        // 处理最后一个间隔
        if (!valuesInInterval.isEmpty()) {
            double avgValue = valuesInInterval.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            result.add(new ChartDataRoutingService.ChartDataPoint(
                currentIntervalStart, avgValue, valuesInInterval.size()));
        }

        return result;
    }

    /**
     * 从记录中提取指标值
     */
    private Double extractValue(DeviceStatusRecord record, String metricName) {
        if ("CPM".equalsIgnoreCase(metricName)) {
            return record.getCpmValue() != null ? record.getCpmValue().doubleValue() : null;
        } else if ("Batvolt".equalsIgnoreCase(metricName) || "Battery".equalsIgnoreCase(metricName)) {
            return record.getBatteryVoltageMv() != null ? record.getBatteryVoltageMv().doubleValue() : null;
        }
        return null;
    }

    /**
     * 获取推荐的数据粒度
     */
    public String getRecommendedGranularity(LocalDateTime startTime, LocalDateTime endTime) {
        long daysBetween = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate());

        if (daysBetween <= MINUTES_30_THRESHOLD) {
            return "30分钟";
        } else if (daysBetween <= HOURLY_THRESHOLD) {
            return "1小时";
        } else if (daysBetween <= SIX_HOURLY_THRESHOLD) {
            return "6小时";
        } else if (daysBetween <= DAILY_THRESHOLD) {
            return "24小时";
        } else {
            return "7天";
        }
    }
}