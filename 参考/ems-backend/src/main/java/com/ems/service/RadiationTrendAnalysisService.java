package com.ems.service;

import com.ems.exception.BusinessException;
import com.ems.exception.ValidationException;
import com.ems.entity.RadiationDeviceStatus;
import com.ems.entity.device.Device;
import com.ems.exception.ErrorCode;
import com.ems.repository.RadiationDeviceStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 辐射监测趋势分析服务
 * 提供辐射水平变化趋势、异常检测、统计分析等功能
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RadiationTrendAnalysisService {

    private final RadiationDeviceStatusRepository radiationDeviceStatusRepository;

    /**
     * 分析辐射水平变化趋势
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param interval  数据间隔（小时）
     * @return 趋势分析结果
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeRadiationTrend(String deviceId,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     int interval) {
        validateTrendParameters(deviceId, startTime, endTime, interval);

        // 获取辐射数据
        List<RadiationDeviceStatus> radiationData = radiationDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (radiationData.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                    "指定时间范围内无辐射数据");
        }

        // 按时间间隔聚合数据
        Map<LocalDateTime, List<RadiationDeviceStatus>> groupedData = radiationData.stream()
                .collect(Collectors.groupingBy(data -> roundToInterval(data.getRecordTime(), interval)));

        // 计算趋势数据
        List<Map<String, Object>> trendData = new ArrayList<>();
        List<Integer> cpmValues = new ArrayList<>();
        List<BigDecimal> doseRates = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<RadiationDeviceStatus>> entry : groupedData.entrySet()) {
            LocalDateTime timeKey = entry.getKey();
            List<RadiationDeviceStatus> intervalData = entry.getValue();

            // 计算间隔内的平均值
            Double avgCpm = intervalData.stream()
                    .filter(data -> data.getCpmValue() != null)
                    .mapToInt(RadiationDeviceStatus::getCpmValue)
                    .average()
                    .orElse(0.0);

            Integer maxCpm = intervalData.stream()
                    .filter(data -> data.getCpmValue() != null)
                    .mapToInt(RadiationDeviceStatus::getCpmValue)
                    .max()
                    .orElse(0);

            Integer minCpm = intervalData.stream()
                    .filter(data -> data.getCpmValue() != null)
                    .mapToInt(RadiationDeviceStatus::getCpmValue)
                    .min()
                    .orElse(0);

            // 计算剂量率
            BigDecimal doseRate = BigDecimal.valueOf(avgCpm)
                    .multiply(new BigDecimal("0.0083")) // CPM转μSv/h
                    .setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("timestamp", timeKey);
            dataPoint.put("averageCpm", avgCpm);
            dataPoint.put("maxCpm", maxCpm);
            dataPoint.put("minCpm", minCpm);
            dataPoint.put("doseRate", doseRate);
            dataPoint.put("dataPoints", intervalData.size());

            trendData.add(dataPoint);
            cpmValues.add(avgCpm.intValue());
            doseRates.add(doseRate);
        }

        // 排序趋势数据
        trendData.sort((a, b) -> ((LocalDateTime) a.get("timestamp")).compareTo((LocalDateTime) b.get("timestamp")));

        // 计算统计指标
        Map<String, Object> statistics = calculateStatistics(cpmValues, doseRates);

        // 检测趋势方向
        String trendDirection = analyzeTrendDirection(cpmValues);

        // 检测异常点
        List<Map<String, Object>> anomalies = detectAnomalies(trendData);

        return Map.of(
                "deviceId", deviceId,
                "timeRange", Map.of("start", startTime, "end", endTime),
                "interval", interval,
                "trendData", trendData,
                "statistics", statistics,
                "trendDirection", trendDirection,
                "anomalies", anomalies,
                "totalDataPoints", radiationData.size()
        );
    }

    /**
     * 检测异常辐射事件
     *
     * @param deviceId 设备ID
     * @param hours    检测时间范围（小时）
     * @return 异常事件列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> detectAnomalousRadiationEvents(String deviceId, int hours) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);

        List<RadiationDeviceStatus> radiationData = radiationDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (radiationData.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算基础统计量
        List<Integer> cpmValues = radiationData.stream()
                .filter(data -> data.getCpmValue() != null)
                .map(RadiationDeviceStatus::getCpmValue)
                .collect(Collectors.toList());

        if (cpmValues.isEmpty()) {
            return Collections.emptyList();
        }

        double mean = cpmValues.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(cpmValues, mean);

        // 异常阈值：均值 ± 3倍标准差
        double upperThreshold = mean + 3 * stdDev;
        double lowerThreshold = Math.max(0, mean - 3 * stdDev);

        // 检测异常事件
        List<Map<String, Object>> anomalies = new ArrayList<>();
        boolean inAnomaly = false;
        LocalDateTime anomalyStart = null;
        int maxAnomalyCpm = 0;

        for (RadiationDeviceStatus data : radiationData) {
            if (data.getCpmValue() == null) continue;

            int cpm = data.getCpmValue();
            boolean isAnomalous = cpm > upperThreshold || cpm < lowerThreshold;

            if (isAnomalous && !inAnomaly) {
                // 异常事件开始
                inAnomaly = true;
                anomalyStart = data.getRecordTime();
                maxAnomalyCpm = cpm;
            } else if (!isAnomalous && inAnomaly) {
                // 异常事件结束
                inAnomaly = false;
                anomalies.add(createAnomalyEvent(anomalyStart, data.getRecordTime(), maxAnomalyCpm, (int) upperThreshold));
            } else if (isAnomalous && inAnomaly) {
                // 持续异常，更新最大值
                maxAnomalyCpm = Math.max(maxAnomalyCpm, cpm);
            }
        }

        // 处理最后持续的异常事件
        if (inAnomaly && anomalyStart != null) {
            anomalies.add(createAnomalyEvent(anomalyStart, endTime, maxAnomalyCpm, (int) upperThreshold));
        }

        return anomalies;
    }

    /**
     * 生成辐射水平统计报告
     *
     * @param deviceId 设备ID
     * @param days     统计天数
     * @return 统计报告
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateRadiationStatisticsReport(String deviceId, int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        List<RadiationDeviceStatus> radiationData = radiationDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (radiationData.isEmpty()) {
            return Map.of("message", "指定时间范围内无辐射数据");
        }

        // 基础统计
        List<Integer> cpmValues = radiationData.stream()
                .filter(data -> data.getCpmValue() != null)
                .map(RadiationDeviceStatus::getCpmValue)
                .collect(Collectors.toList());

        if (cpmValues.isEmpty()) {
            return Map.of("message", "无有效的CPM数据");
        }

        // 计算详细统计
        Map<String, Object> detailedStats = calculateDetailedStatistics(cpmValues);

        // 按小时统计
        Map<Integer, Map<String, Object>> hourlyStats = calculateHourlyStatistics(radiationData);

        // 风险等级统计
        Map<String, Integer> riskLevelStats = calculateRiskLevelStatistics(cpmValues);

        // 数据质量评估
        Map<String, Object> dataQuality = assessDataQuality(radiationData, days);

        // 趋势分析
        String weeklyTrend = analyzeWeeklyTrend(cpmValues, days);

        return Map.of(
                "deviceId", deviceId,
                "statisticsPeriod", Map.of("start", startTime, "end", endTime, "days", days),
                "basicStatistics", detailedStats,
                "hourlyStatistics", hourlyStats,
                "riskLevelDistribution", riskLevelStats,
                "dataQuality", dataQuality,
                "weeklyTrend", weeklyTrend,
                "summary", generateSummaryStatistics(detailedStats, riskLevelStats)
        );
    }

    /**
     * 比较不同时间段的辐射水平
     *
     * @param deviceId 设备ID
     * @param periods  时间段列表（每个包含名称、开始时间、结束时间）
     * @return 比较结果
     */
    @Transactional(readOnly = true)
    public Map<String, Object> compareRadiationLevels(String deviceId, List<Map<String, LocalDateTime>> periods) {
        List<Map<String, Object>> comparisonResults = new ArrayList<>();

        for (Map<String, LocalDateTime> period : periods) {
            String periodName = (String) period.keySet().iterator().next();
            // 这里简化处理，实际应该有更好的数据结构
            LocalDateTime startTime = period.get("start");
            LocalDateTime endTime = period.get("end");

            try {
                Map<String, Object> periodStats = analyzeRadiationTrend(deviceId, startTime, endTime, 1);
                periodStats.put("periodName", periodName);
                comparisonResults.add(periodStats);
            } catch (Exception e) {
                log.warn("分析时间段 {} 失败: {}", periodName, e.getMessage());
            }
        }

        // 生成比较报告
        return Map.of(
                "deviceId", deviceId,
                "comparisons", comparisonResults,
                "analysis", generateComparisonAnalysis(comparisonResults)
        );
    }

    /**
     * 计算统计指标
     */
    private Map<String, Object> calculateStatistics(List<Integer> cpmValues, List<BigDecimal> doseRates) {
        if (cpmValues.isEmpty()) {
            return Map.of();
        }

        double mean = cpmValues.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double median = calculateMedian(cpmValues);
        int min = cpmValues.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = cpmValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        double stdDev = calculateStandardDeviation(cpmValues, mean);

        BigDecimal avgDoseRate = doseRates.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(doseRates.size()), 4, RoundingMode.HALF_UP);

        return Map.of(
                "mean", Math.round(mean * 100.0) / 100.0,
                "median", Math.round(median * 100.0) / 100.0,
                "min", min,
                "max", max,
                "standardDeviation", Math.round(stdDev * 100.0) / 100.0,
                "averageDoseRate", avgDoseRate,
                "dataPoints", cpmValues.size(),
                "coefficientOfVariation", mean > 0 ? (stdDev / mean) : 0.0
        );
    }

    /**
     * 分析趋势方向
     */
    private String analyzeTrendDirection(List<Integer> values) {
        if (values.size() < 2) {
            return "insufficient_data";
        }

        // 简单线性回归分析趋势
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        if (Math.abs(slope) < 0.01) {
            return "stable";
        } else if (slope > 0) {
            return slope > 1.0 ? "increasing_rapidly" : "increasing";
        } else {
            return slope < -1.0 ? "decreasing_rapidly" : "decreasing";
        }
    }

    /**
     * 检测异常点
     */
    private List<Map<String, Object>> detectAnomalies(List<Map<String, Object>> trendData) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        if (trendData.size() < 5) {
            return anomalies;
        }

        // 计算移动平均和标准差
        for (int i = 2; i < trendData.size() - 2; i++) {
            Map<String, Object> current = trendData.get(i);
            double currentCpm = (Double) current.get("averageCpm");

            // 计算前后各2个点的平均值
            double neighborAvg = 0;
            int count = 0;
            for (int j = i - 2; j <= i + 2; j++) {
                if (j >= 0 && j < trendData.size() && j != i) {
                    neighborAvg += (Double) trendData.get(j).get("averageCpm");
                    count++;
                }
            }
            neighborAvg /= count;

            // 如果当前值与邻居平均值的差异超过30%，标记为异常
            if (Math.abs(currentCpm - neighborAvg) / neighborAvg > 0.3) {
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("timestamp", current.get("timestamp"));
                anomaly.put("value", currentCpm);
                anomaly.put("expectedValue", neighborAvg);
                anomaly.put("deviationPercent", Math.round((currentCpm - neighborAvg) / neighborAvg * 10000.0) / 100.0);
                anomaly.put("type", currentCpm > neighborAvg ? "spike" : "drop");
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    /**
     * 创建异常事件记录
     */
    private Map<String, Object> createAnomalyEvent(LocalDateTime startTime, LocalDateTime endTime, int maxCpm, int threshold) {
        Map<String, Object> event = new HashMap<>();
        event.put("startTime", startTime);
        event.put("endTime", endTime);
        event.put("duration", ChronoUnit.MINUTES.between(startTime, endTime));
        event.put("maxCpm", maxCpm);
        event.put("threshold", threshold);
        event.put("severity", determineSeverity(maxCpm, threshold));
        return event;
    }

    /**
     * 确定异常严重程度
     */
    private String determineSeverity(int maxCpm, int threshold) {
        double ratio = (double) maxCpm / threshold;
        if (ratio > 5.0) return "critical";
        if (ratio > 3.0) return "high";
        if (ratio > 2.0) return "medium";
        return "low";
    }

    /**
     * 计算标准差
     */
    private double calculateStandardDeviation(List<Integer> values, double mean) {
        double sumSquaredDiff = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();

        return Math.sqrt(sumSquaredDiff / values.size());
    }

    /**
     * 计算中位数
     */
    private double calculateMedian(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();

        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    /**
     * 按时间间隔聚合
     */
    private LocalDateTime roundToInterval(LocalDateTime time, int intervalHours) {
        LocalDateTime rounded = time;
        rounded = rounded.minusHours(rounded.getHour() % intervalHours);
        rounded = rounded.minusMinutes(rounded.getMinute());
        rounded = rounded.minusSeconds(rounded.getSecond());
        rounded = rounded.minusNanos(rounded.getNano());
        return rounded;
    }

    /**
     * 验证趋势分析参数
     */
    private void validateTrendParameters(String deviceId, LocalDateTime startTime, LocalDateTime endTime, int interval) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new ValidationException("设备ID不能为空");
        }

        if (startTime == null || endTime == null) {
            throw new ValidationException("时间范围不能为空");
        }

        if (startTime.isAfter(endTime)) {
            throw new ValidationException("开始时间不能晚于结束时间");
        }

        if (interval <= 0) {
            throw new ValidationException("时间间隔必须大于0");
        }

        if (ChronoUnit.HOURS.between(startTime, endTime) < interval) {
            throw new ValidationException("时间间隔不能大于时间范围");
        }
    }

    /**
     * 计算详细统计
     */
    private Map<String, Object> calculateDetailedStatistics(List<Integer> cpmValues) {
        // 这里可以添加更多详细的统计计算
        return Map.of(
                "count", cpmValues.size(),
                "mean", cpmValues.stream().mapToInt(Integer::intValue).average().orElse(0.0),
                "median", calculateMedian(cpmValues),
                "min", cpmValues.stream().mapToInt(Integer::intValue).min().orElse(0),
                "max", cpmValues.stream().mapToInt(Integer::intValue).max().orElse(0)
        );
    }

    /**
     * 计算每小时统计
     */
    private Map<Integer, Map<String, Object>> calculateHourlyStatistics(List<RadiationDeviceStatus> radiationData) {
        return radiationData.stream()
                .filter(data -> data.getCpmValue() != null)
                .collect(Collectors.groupingBy(
                        data -> data.getRecordTime().getHour(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                hourData -> {
                                    double avg = hourData.stream()
                                            .mapToInt(RadiationDeviceStatus::getCpmValue)
                                            .average().orElse(0.0);
                                    return Map.of(
                                            "average", avg,
                                            "count", hourData.size()
                                    );
                                }
                        )
                ));
    }

    /**
     * 计算风险等级统计
     */
    private Map<String, Integer> calculateRiskLevelStatistics(List<Integer> cpmValues) {
        Map<String, Integer> riskLevels = new HashMap<>();
        riskLevels.put("normal", 0);
        riskLevels.put("elevated", 0);
        riskLevels.put("high", 0);
        riskLevels.put("critical", 0);

        for (int cpm : cpmValues) {
            if (cpm <= 50) {
                riskLevels.put("normal", riskLevels.get("normal") + 1);
            } else if (cpm <= 100) {
                riskLevels.put("elevated", riskLevels.get("elevated") + 1);
            } else if (cpm <= 500) {
                riskLevels.put("high", riskLevels.get("high") + 1);
            } else {
                riskLevels.put("critical", riskLevels.get("critical") + 1);
            }
        }

        return riskLevels;
    }

    /**
     * 评估数据质量
     */
    private Map<String, Object> assessDataQuality(List<RadiationDeviceStatus> radiationData, int days) {
        int expectedDataPoints = days * 24 * 60; // 假设每分钟一个数据点
        int actualDataPoints = radiationData.size();
        double completeness = (double) actualDataPoints / expectedDataPoints;

        int validCpmData = (int) radiationData.stream()
                .filter(data -> data.getCpmValue() != null && data.getCpmValue() >= 0)
                .count();

        double dataValidity = (double) validCpmData / actualDataPoints;

        return Map.of(
                "completeness", Math.round(completeness * 10000.0) / 100.0,
                "validity", Math.round(dataValidity * 10000.0) / 100.0,
                "totalPoints", actualDataPoints,
                "expectedPoints", expectedDataPoints
        );
    }

    /**
     * 分析周趋势
     */
    private String analyzeWeeklyTrend(List<Integer> cpmValues, int days) {
        if (cpmValues.size() < 7) {
            return "insufficient_data";
        }

        // 简单分析：比较前半周和后半周的平均值
        int midPoint = cpmValues.size() / 2;
        double firstHalfAvg = cpmValues.subList(0, midPoint).stream()
                .mapToInt(Integer::intValue).average().orElse(0.0);
        double secondHalfAvg = cpmValues.subList(midPoint, cpmValues.size()).stream()
                .mapToInt(Integer::intValue).average().orElse(0.0);

        double change = (secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100;

        if (Math.abs(change) < 5) {
            return "stable";
        } else if (change > 0) {
            return "increasing";
        } else {
            return "decreasing";
        }
    }

    /**
     * 生成汇总统计
     */
    private Map<String, Object> generateSummaryStatistics(Map<String, Object> detailedStats, Map<String, Integer> riskLevelStats) {
        return Map.of(
                "averageLevel", detailedStats.get("mean"),
                "maxLevel", detailedStats.get("max"),
                "dataQuality", "good", // 简化处理
                "riskAssessment", riskLevelStats.get("critical") > 0 ? "high_risk" :
                                   riskLevelStats.get("high") > 0 ? "medium_risk" : "low_risk"
        );
    }

    /**
     * 生成比较分析
     */
    private Map<String, Object> generateComparisonAnalysis(List<Map<String, Object>> comparisonResults) {
        if (comparisonResults.size() < 2) {
            return Map.of("message", "需要至少两个时间段进行比较");
        }

        // 简化的比较分析
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("periodsCount", comparisonResults.size());

        // 找出最高和最低的平均值
        double maxAvg = 0;
        double minAvg = Double.MAX_VALUE;
        String maxPeriod = "";
        String minPeriod = "";

        for (Map<String, Object> result : comparisonResults) {
            Map<String, Object> stats = (Map<String, Object>) result.get("statistics");
            Double avg = (Double) stats.get("mean");
            String periodName = (String) result.get("periodName");

            if (avg != null) {
                if (avg > maxAvg) {
                    maxAvg = avg;
                    maxPeriod = periodName;
                }
                if (avg < minAvg) {
                    minAvg = avg;
                    minPeriod = periodName;
                }
            }
        }

        analysis.put("highestPeriod", Map.of("period", maxPeriod, "average", maxAvg));
        analysis.put("lowestPeriod", Map.of("period", minPeriod, "average", minAvg));

        return analysis;
    }
}