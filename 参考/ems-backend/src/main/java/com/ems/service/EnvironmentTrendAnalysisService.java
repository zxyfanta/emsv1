package com.ems.service;

import com.ems.exception.BusinessException;
import com.ems.exception.ValidationException;
import com.ems.entity.EnvironmentDeviceStatus;
import com.ems.exception.ErrorCode;
import com.ems.repository.EnvironmentDeviceStatusRepository;
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
 * 环境监测站趋势分析服务
 * 提供环境数据趋势分析、异常检测、统计分析等功能
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentTrendAnalysisService {

    private final EnvironmentDeviceStatusRepository environmentDeviceStatusRepository;

    // 环境质量评估标准
    private static final Map<String, Map<String, BigDecimal>> ENVIRONMENT_STANDARDS = Map.of(
        "temperature", Map.of(
            "excellent_low", new BigDecimal("18.0"),
            "excellent_high", new BigDecimal("26.0"),
            "good_low", new BigDecimal("15.0"),
            "good_high", new BigDecimal("30.0"),
            "fair_low", new BigDecimal("10.0"),
            "fair_high", new BigDecimal("35.0"),
            "poor_low", new BigDecimal("5.0"),
            "poor_high", new BigDecimal("40.0")
        ),
        "wetness", Map.of(
            "excellent_low", new BigDecimal("40.0"),
            "excellent_high", new BigDecimal("60.0"),
            "good_low", new BigDecimal("30.0"),
            "good_high", new BigDecimal("70.0"),
            "fair_low", new BigDecimal("20.0"),
            "fair_high", new BigDecimal("80.0"),
            "poor_low", new BigDecimal("10.0"),
            "poor_high", new BigDecimal("90.0")
        ),
        "windspeed", Map.of(
            "excellent_low", new BigDecimal("0.5"),
            "excellent_high", new BigDecimal("3.0"),
            "good_low", new BigDecimal("0.3"),
            "good_high", new BigDecimal("5.0"),
            "fair_low", new BigDecimal("0.1"),
            "fair_high", new BigDecimal("8.0"),
            "poor_low", new BigDecimal("0.0"),
            "poor_high", new BigDecimal("15.0")
        )
    );

    /**
     * 分析环境数据趋势
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param interval  数据间隔（小时）
     * @return 趋势分析结果
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeEnvironmentTrend(String deviceId,
                                                        LocalDateTime startTime,
                                                        LocalDateTime endTime,
                                                        int interval) {
        validateTrendParameters(deviceId, startTime, endTime, interval);

        // 获取环境数据
        List<EnvironmentDeviceStatus> environmentData = environmentDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (environmentData.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                    "指定时间范围内无环境数据");
        }

        // 按时间间隔聚合数据
        Map<LocalDateTime, List<EnvironmentDeviceStatus>> groupedData = environmentData.stream()
                .collect(Collectors.groupingBy(data -> roundToInterval(data.getRecordTime(), interval)));

        // 计算各指标的趋势数据
        Map<String, List<Map<String, Object>>> allTrends = new HashMap<>();
        allTrends.put("temperature", calculateMetricTrend(groupedData, "temperature"));
        allTrends.put("wetness", calculateMetricTrend(groupedData, "wetness"));
        allTrends.put("windSpeed", calculateMetricTrend(groupedData, "windSpeed"));
        allTrends.put("totalIndex", calculateMetricTrend(groupedData, "totalIndex"));

        // 计算综合环境指数趋势
        List<Map<String, Object>> combinedTrend = combineEnvironmentTrends(allTrends);

        // 生成统计分析
        Map<String, Object> statistics = calculateEnvironmentStatistics(environmentData);

        // 检测环境异常
        List<Map<String, Object>> anomalies = detectEnvironmentAnomalies(combinedTrend);

        // 环境质量评估
        Map<String, Object> qualityAssessment = assessEnvironmentQuality(statistics);

        return Map.of(
                "deviceId", deviceId,
                "timeRange", Map.of("start", startTime, "end", endTime),
                "interval", interval,
                "trends", allTrends,
                "combinedTrend", combinedTrend,
                "statistics", statistics,
                "anomalies", anomalies,
                "qualityAssessment", qualityAssessment,
                "totalDataPoints", environmentData.size()
        );
    }

    /**
     * 分析环境质量指数趋势
     *
     * @param deviceId 设备ID
     * @param days    分析天数
     * @return 环境质量指数趋势
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeEnvironmentQualityTrend(String deviceId, int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        Map<String, Object> trendAnalysis = analyzeEnvironmentTrend(deviceId, startTime, endTime, 6);

        // 专门分析综合环境指数的趋势
        List<Map<String, Object>> qualityTrend = (List<Map<String, Object>>) trendAnalysis.get("combinedTrend");

        // 计算质量等级分布
        Map<String, Integer> qualityDistribution = calculateQualityDistribution(qualityTrend);

        // 分析质量变化趋势
        String qualityTrendDirection = analyzeQualityTrendDirection(qualityTrend);

        return Map.of(
                "deviceId", deviceId,
                "analysisPeriod", Map.of("start", startTime, "end", endTime, "days", days),
                "qualityTrend", qualityTrend,
                "qualityDistribution", qualityDistribution,
                "trendDirection", qualityTrendDirection,
                "currentQuality", qualityTrend.isEmpty() ? "unknown" :
                    ((String) qualityTrend.get(qualityTrend.size() - 1).get("quality"))
        );
    }

    /**
     * 检测环境异常事件
     *
     * @param deviceId 设备ID
     * @param hours    检测时间范围（小时）
     * @return 异常事件列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> detectEnvironmentAnomalies(String deviceId, int hours) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);

        List<EnvironmentDeviceStatus> environmentData = environmentDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (environmentData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> anomalies = new ArrayList<>();

        // 检测温度异常
        anomalies.addAll(detectTemperatureAnomalies(environmentData));

        // 检测湿度异常
        anomalies.addAll(detectHumidityAnomalies(environmentData));

        // 检测风速异常
        anomalies.addAll(detectWindSpeedAnomalies(environmentData));

        // 检测综合指数异常
        anomalies.addAll(detectTotalIndexAnomalies(environmentData));

        // 按时间排序
        anomalies.sort((a, b) -> ((LocalDateTime) a.get("timestamp")).compareTo((LocalDateTime) b.get("timestamp")));

        return anomalies;
    }

    /**
     * 生成环境监测统计报告
     *
     * @param deviceId 设备ID
     * @param days     统计天数
     * @return 统计报告
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateEnvironmentStatisticsReport(String deviceId, int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        List<EnvironmentDeviceStatus> environmentData = environmentDeviceStatusRepository
                .findByDeviceIdAndRecordTimeBetweenOrderByRecordTime(deviceId, startTime, endTime);

        if (environmentData.isEmpty()) {
            return Map.of("message", "指定时间范围内无环境数据");
        }

        // 基础统计
        Map<String, Object> basicStats = calculateEnvironmentStatistics(environmentData);

        // 按小时统计
        Map<Integer, Map<String, Object>> hourlyStats = calculateHourlyEnvironmentStatistics(environmentData);

        // 按日统计
        Map<Integer, Map<String, Object>> dailyStats = calculateDailyEnvironmentStatistics(environmentData);

        // 环境质量分布
        Map<String, Integer> qualityDistribution = calculateQualityDistributionFromData(environmentData);

        // 舒适度评估
        Map<String, Object> comfortAssessment = assessComfortLevel(environmentData);

        // 极端天气统计
        Map<String, Object> extremeWeatherStats = calculateExtremeWeatherStatistics(environmentData);

        return Map.of(
                "deviceId", deviceId,
                "statisticsPeriod", Map.of("start", startTime, "end", endTime, "days", days),
                "basicStatistics", basicStats,
                "hourlyStatistics", hourlyStats,
                "dailyStatistics", dailyStats,
                "qualityDistribution", qualityDistribution,
                "comfortAssessment", comfortAssessment,
                "extremeWeatherStatistics", extremeWeatherStats
        );
    }

    /**
     * 计算指标趋势
     */
    private List<Map<String, Object>> calculateMetricTrend(Map<LocalDateTime, List<EnvironmentDeviceStatus>> groupedData, String metric) {
        List<Map<String, Object>> trendData = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<EnvironmentDeviceStatus>> entry : groupedData.entrySet()) {
            LocalDateTime timeKey = entry.getKey();
            List<EnvironmentDeviceStatus> intervalData = entry.getValue();

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("timestamp", timeKey);
            dataPoint.put("dataPoints", intervalData.size());

            // 根据指标类型计算相应的统计值
            switch (metric) {
                case "temperature":
                    calculateTemperatureStats(intervalData, dataPoint);
                    break;
                case "wetness":
                    calculateHumidityStats(intervalData, dataPoint);
                    break;
                case "windSpeed":
                    calculateWindSpeedStats(intervalData, dataPoint);
                    break;
                case "totalIndex":
                    calculateTotalIndexStats(intervalData, dataPoint);
                    break;
            }

            trendData.add(dataPoint);
        }

        // 排序趋势数据
        trendData.sort((a, b) -> ((LocalDateTime) a.get("timestamp")).compareTo((LocalDateTime) b.get("timestamp")));
        return trendData;
    }

    /**
     * 计算温度统计
     */
    private void calculateTemperatureStats(List<EnvironmentDeviceStatus> data, Map<String, Object> dataPoint) {
        List<Double> temperatures = data.stream()
                .filter(d -> d.getTemperature() != null)
                .map(d -> d.getTemperature())
                .collect(Collectors.toList());

        if (!temperatures.isEmpty()) {
            double avg = temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = temperatures.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double min = temperatures.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

            dataPoint.put("average", avg);
            dataPoint.put("max", max);
            dataPoint.put("min", min);
            dataPoint.put("range", max - min);
            dataPoint.put("quality", assessTemperatureQuality(avg));
        }
    }

    /**
     * 计算湿度统计
     */
    private void calculateHumidityStats(List<EnvironmentDeviceStatus> data, Map<String, Object> dataPoint) {
        List<Double> humidity = data.stream()
                .filter(d -> d.getWetness() != null)
                .map(d -> d.getWetness())
                .collect(Collectors.toList());

        if (!humidity.isEmpty()) {
            double avg = humidity.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = humidity.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double min = humidity.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

            dataPoint.put("average", avg);
            dataPoint.put("max", max);
            dataPoint.put("min", min);
            dataPoint.put("range", max - min);
            dataPoint.put("quality", assessHumidityQuality(avg));
        }
    }

    /**
     * 计算风速统计
     */
    private void calculateWindSpeedStats(List<EnvironmentDeviceStatus> data, Map<String, Object> dataPoint) {
        List<Double> windSpeeds = data.stream()
                .filter(d -> d.getWindSpeed() != null)
                .map(d -> d.getWindSpeed())
                .collect(Collectors.toList());

        if (!windSpeeds.isEmpty()) {
            double avg = windSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = windSpeeds.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double min = windSpeeds.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

            dataPoint.put("average", avg);
            dataPoint.put("max", max);
            dataPoint.put("min", min);
            dataPoint.put("range", max - min);
            dataPoint.put("quality", assessWindSpeedQuality(avg));
        }
    }

    /**
     * 计算综合指数统计
     */
    private void calculateTotalIndexStats(List<EnvironmentDeviceStatus> data, Map<String, Object> dataPoint) {
        List<BigDecimal> totalIndexes = data.stream()
                .filter(d -> d.getTotalEnvironmentIndex() != null)
                .map(d -> d.getTotalEnvironmentIndex())
                .collect(Collectors.toList());

        if (!totalIndexes.isEmpty()) {
            BigDecimal avg = totalIndexes.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(totalIndexes.size()), 2, RoundingMode.HALF_UP);

            BigDecimal max = totalIndexes.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = totalIndexes.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            dataPoint.put("average", avg);
            dataPoint.put("max", max);
            dataPoint.put("min", min);
            dataPoint.put("range", max.subtract(min));
            dataPoint.put("quality", assessIndexQuality(avg));
        }
    }

    /**
     * 合并环境指标趋势
     */
    private List<Map<String, Object>> combineEnvironmentTrends(Map<String, List<Map<String, Object>>> allTrends) {
        List<Map<String, Object>> combinedTrend = new ArrayList<>();

        // 获取所有时间点
        Set<LocalDateTime> allTimestamps = new TreeSet<>();
        allTrends.values().stream()
                .flatMap(List::stream)
                .forEach(trend -> allTimestamps.add((LocalDateTime) trend.get("timestamp")));

        for (LocalDateTime timestamp : allTimestamps) {
            Map<String, Object> combinedPoint = new HashMap<>();
            combinedPoint.put("timestamp", timestamp);

            // 合并各指标在此时间点的数据
            for (Map.Entry<String, List<Map<String, Object>>> entry : allTrends.entrySet()) {
                String metric = entry.getKey();
                List<Map<String, Object>> metricTrend = entry.getValue();

                // 查找此时间点的数据
                Optional<Map<String, Object>> pointData = metricTrend.stream()
                        .filter(point -> timestamp.equals(point.get("timestamp")))
                        .findFirst();

                if (pointData.isPresent()) {
                    combinedPoint.put(metric, pointData.get());
                }
            }

            combinedTrend.add(combinedPoint);
        }

        return combinedTrend;
    }

    /**
     * 计算环境统计
     */
    private Map<String, Object> calculateEnvironmentStatistics(List<EnvironmentDeviceStatus> environmentData) {
        Map<String, Object> stats = new HashMap<>();

        // 温度统计
        List<Double> temperatures = environmentData.stream()
                .filter(d -> d.getTemperature() != null)
                .map(d -> d.getTemperature())
                .collect(Collectors.toList());

        if (!temperatures.isEmpty()) {
            stats.put("temperature", Map.of(
                    "mean", temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    "max", temperatures.stream().mapToDouble(Double::doubleValue).max().orElse(0.0),
                    "min", temperatures.stream().mapToDouble(Double::doubleValue).min().orElse(0.0),
                    "count", temperatures.size()
            ));
        }

        // 湿度统计
        List<Double> humidity = environmentData.stream()
                .filter(d -> d.getWetness() != null)
                .map(d -> d.getWetness())
                .collect(Collectors.toList());

        if (!humidity.isEmpty()) {
            stats.put("wetness", Map.of(
                    "mean", humidity.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    "max", humidity.stream().mapToDouble(Double::doubleValue).max().orElse(0.0),
                    "min", humidity.stream().mapToDouble(Double::doubleValue).min().orElse(0.0),
                    "count", humidity.size()
            ));
        }

        // 风速统计
        List<Double> windSpeeds = environmentData.stream()
                .filter(d -> d.getWindSpeed() != null)
                .map(d -> d.getWindSpeed())
                .collect(Collectors.toList());

        if (!windSpeeds.isEmpty()) {
            stats.put("windSpeed", Map.of(
                    "mean", windSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    "max", windSpeeds.stream().mapToDouble(Double::doubleValue).max().orElse(0.0),
                    "min", windSpeeds.stream().mapToDouble(Double::doubleValue).min().orElse(0.0),
                    "count", windSpeeds.size()
            ));
        }

        // 综合指数统计
        List<BigDecimal> totalIndexes = environmentData.stream()
                .filter(d -> d.getTotalEnvironmentIndex() != null)
                .map(d -> d.getTotalEnvironmentIndex())
                .collect(Collectors.toList());

        if (!totalIndexes.isEmpty()) {
            stats.put("totalIndex", Map.of(
                    "mean", totalIndexes.stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(totalIndexes.size()), 2, RoundingMode.HALF_UP),
                    "max", totalIndexes.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                    "min", totalIndexes.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                    "count", totalIndexes.size()
            ));
        }

        return stats;
    }

    /**
     * 评估环境质量
     */
    private Map<String, Object> assessEnvironmentQuality(Map<String, Object> statistics) {
        Map<String, Object> assessment = new HashMap<>();
        Map<String, String> qualities = new HashMap<>();
        String overallQuality = "unknown";

        if (statistics.containsKey("temperature")) {
            Map<String, Object> tempStats = (Map<String, Object>) statistics.get("temperature");
            double tempAvg = (Double) tempStats.get("mean");
            qualities.put("temperature", assessTemperatureQuality(tempAvg));
        }

        if (statistics.containsKey("wetness")) {
            Map<String, Object> humidityStats = (Map<String, Object>) statistics.get("wetness");
            double humidityAvg = (Double) humidityStats.get("mean");
            qualities.put("wetness", assessHumidityQuality(humidityAvg));
        }

        if (statistics.containsKey("windSpeed")) {
            Map<String, Object> windStats = (Map<String, Object>) statistics.get("windSpeed");
            double windAvg = (Double) windStats.get("mean");
            qualities.put("windSpeed", assessWindSpeedQuality(windAvg));
        }

        if (statistics.containsKey("totalIndex")) {
            Map<String, Object> indexStats = (Map<String, Object>) statistics.get("totalIndex");
            overallQuality = assessIndexQuality((BigDecimal) indexStats.get("mean"));
        }

        assessment.put("individualQualities", qualities);
        assessment.put("overallQuality", overallQuality);
        assessment.put("assessmentTime", LocalDateTime.now());

        return assessment;
    }

    /**
     * 评估温度质量
     */
    private String assessTemperatureQuality(double temperature) {
        Map<String, BigDecimal> standards = ENVIRONMENT_STANDARDS.get("temperature");
        if (standards == null) return "unknown";

        BigDecimal temp = BigDecimal.valueOf(temperature);

        if (temp.compareTo(standards.get("excellent_low")) >= 0 &&
            temp.compareTo(standards.get("excellent_high")) <= 0) {
            return "excellent";
        } else if (temp.compareTo(standards.get("good_low")) >= 0 &&
                   temp.compareTo(standards.get("good_high")) <= 0) {
            return "good";
        } else if (temp.compareTo(standards.get("fair_low")) >= 0 &&
                   temp.compareTo(standards.get("fair_high")) <= 0) {
            return "fair";
        } else {
            return "poor";
        }
    }

    /**
     * 评估湿度质量
     */
    private String assessHumidityQuality(double humidity) {
        Map<String, BigDecimal> standards = ENVIRONMENT_STANDARDS.get("wetness");
        if (standards == null) return "unknown";

        BigDecimal hum = BigDecimal.valueOf(humidity);

        if (hum.compareTo(standards.get("excellent_low")) >= 0 &&
            hum.compareTo(standards.get("excellent_high")) <= 0) {
            return "excellent";
        } else if (hum.compareTo(standards.get("good_low")) >= 0 &&
                   hum.compareTo(standards.get("good_high")) <= 0) {
            return "good";
        } else if (hum.compareTo(standards.get("fair_low")) >= 0 &&
                   hum.compareTo(standards.get("fair_high")) <= 0) {
            return "fair";
        } else {
            return "poor";
        }
    }

    /**
     * 评估风速质量
     */
    private String assessWindSpeedQuality(double windSpeed) {
        Map<String, BigDecimal> standards = ENVIRONMENT_STANDARDS.get("windspeed");
        if (standards == null) return "unknown";

        BigDecimal wind = BigDecimal.valueOf(windSpeed);

        if (wind.compareTo(standards.get("excellent_low")) >= 0 &&
            wind.compareTo(standards.get("excellent_high")) <= 0) {
            return "excellent";
        } else if (wind.compareTo(standards.get("good_low")) >= 0 &&
                   wind.compareTo(standards.get("good_high")) <= 0) {
            return "good";
        } else if (wind.compareTo(standards.get("fair_low")) >= 0 &&
                   wind.compareTo(standards.get("fair_high")) <= 0) {
            return "fair";
        } else {
            return "poor";
        }
    }

    /**
     * 评估指数质量
     */
    private String assessIndexQuality(BigDecimal index) {
        if (index == null) return "unknown";

        if (index.compareTo(new BigDecimal("150")) >= 0) {
            return "excellent";
        } else if (index.compareTo(new BigDecimal("100")) >= 0) {
            return "good";
        } else if (index.compareTo(new BigDecimal("50")) >= 0) {
            return "fair";
        } else {
            return "poor";
        }
    }

    /**
     * 检测环境异常
     */
    private List<Map<String, Object>> detectEnvironmentAnomalies(List<Map<String, Object>> combinedTrend) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        for (Map<String, Object> dataPoint : combinedTrend) {
            LocalDateTime timestamp = (LocalDateTime) dataPoint.get("timestamp");

            // 检查温度异常
            if (dataPoint.containsKey("temperature")) {
                Map<String, Object> tempData = (Map<String, Object>) dataPoint.get("temperature");
                double maxTemp = (Double) tempData.get("max");
                double minTemp = (Double) tempData.get("min");

                if (maxTemp > 45.0) { // 高温异常
                    anomalies.add(createEnvironmentAnomaly(timestamp, "temperature", "high", maxTemp));
                }
                if (minTemp < -10.0) { // 低温异常
                    anomalies.add(createEnvironmentAnomaly(timestamp, "temperature", "low", minTemp));
                }
            }

            // 检查湿度异常
            if (dataPoint.containsKey("wetness")) {
                Map<String, Object> humidityData = (Map<String, Object>) dataPoint.get("wetness");
                double maxHumidity = (Double) humidityData.get("max");
                double minHumidity = (Double) humidityData.get("min");

                if (maxHumidity > 95.0) { // 过高湿度
                    anomalies.add(createEnvironmentAnomaly(timestamp, "wetness", "high", maxHumidity));
                }
                if (minHumidity < 10.0) { // 过低湿度
                    anomalies.add(createEnvironmentAnomaly(timestamp, "wetness", "low", minHumidity));
                }
            }

            // 检查风速异常
            if (dataPoint.containsKey("windSpeed")) {
                Map<String, Object> windData = (Map<String, Object>) dataPoint.get("windSpeed");
                double maxWindSpeed = (Double) windData.get("max");

                if (maxWindSpeed > 20.0) { // 强风异常
                    anomalies.add(createEnvironmentAnomaly(timestamp, "windSpeed", "high", maxWindSpeed));
                }
            }
        }

        return anomalies;
    }

    /**
     * 创建环境异常记录
     */
    private Map<String, Object> createEnvironmentAnomaly(LocalDateTime timestamp, String metric, String type, double value) {
        Map<String, Object> anomaly = new HashMap<>();
        anomaly.put("timestamp", timestamp);
        anomaly.put("metric", metric);
        anomaly.put("type", type);
        anomaly.put("value", value);
        anomaly.put("severity", determineAnomalySeverity(metric, type, value));
        return anomaly;
    }

    /**
     * 确定异常严重程度
     */
    private String determineAnomalySeverity(String metric, String type, double value) {
        switch (metric) {
            case "temperature":
                if (type.equals("high")) {
                    return value > 50.0 ? "critical" : "high";
                } else {
                    return value < -20.0 ? "critical" : "high";
                }
            case "wetness":
                return value > 98.0 || value < 5.0 ? "medium" : "low";
            case "windSpeed":
                return value > 25.0 ? "high" : "medium";
            default:
                return "low";
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
    }

    /**
     * 计算质量分布
     */
    private Map<String, Integer> calculateQualityDistribution(List<Map<String, Object>> qualityTrend) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("excellent", 0);
        distribution.put("good", 0);
        distribution.put("fair", 0);
        distribution.put("poor", 0);

        for (Map<String, Object> point : qualityTrend) {
            String quality = (String) point.get("quality");
            if (quality != null && distribution.containsKey(quality)) {
                distribution.put(quality, distribution.get(quality) + 1);
            }
        }

        return distribution;
    }

    /**
     * 分析质量趋势方向
     */
    private String analyzeQualityTrendDirection(List<Map<String, Object>> qualityTrend) {
        if (qualityTrend.size() < 2) {
            return "insufficient_data";
        }

        // 简化分析：比较前半段和后半段的质量
        int midPoint = qualityTrend.size() / 2;
        Map<String, Integer> firstHalfQuality = new HashMap<>();
        Map<String, Integer> secondHalfQuality = new HashMap<>();

        for (int i = 0; i < qualityTrend.size(); i++) {
            String quality = (String) qualityTrend.get(i).get("quality");
            if (quality != null) {
                if (i < midPoint) {
                    firstHalfQuality.put(quality, firstHalfQuality.getOrDefault(quality, 0) + 1);
                } else {
                    secondHalfQuality.put(quality, secondHalfQuality.getOrDefault(quality, 0) + 1);
                }
            }
        }

        // 比较质量分布变化
        String firstHalfDominant = getDominantQuality(firstHalfQuality);
        String secondHalfDominant = getDominantQuality(secondHalfQuality);

        if (firstHalfDominant.equals(secondHalfDominant)) {
            return "stable";
        } else if (isQualityImproved(firstHalfDominant, secondHalfDominant)) {
            return "improving";
        } else {
            return "declining";
        }
    }

    /**
     * 获取主导质量
     */
    private String getDominantQuality(Map<String, Integer> qualityCounts) {
        return qualityCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
    }

    /**
     * 判断质量是否改善
     */
    private boolean isQualityImproved(String oldQuality, String newQuality) {
        Map<String, Integer> qualityOrder = Map.of(
                "poor", 0,
                "fair", 1,
                "good", 2,
                "excellent", 3
        );

        return qualityOrder.getOrDefault(newQuality, 0) > qualityOrder.getOrDefault(oldQuality, 0);
    }

    /**
     * 从数据计算质量分布
     */
    private Map<String, Integer> calculateQualityDistributionFromData(List<EnvironmentDeviceStatus> environmentData) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("excellent", 0);
        distribution.put("good", 0);
        distribution.put("fair", 0);
        distribution.put("poor", 0);

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getTotalEnvironmentIndex() != null) {
                String quality = assessIndexQuality(data.getTotalEnvironmentIndex());
                distribution.put(quality, distribution.get(quality) + 1);
            }
        }

        return distribution;
    }

    /**
     * 评估舒适度
     */
    private Map<String, Object> assessComfortLevel(List<EnvironmentDeviceStatus> environmentData) {
        Map<String, Object> comfortAssessment = new HashMap<>();
        List<String> comfortablePeriods = new ArrayList<>();
        List<String> uncomfortablePeriods = new ArrayList<>();

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getTemperature() != null && data.getWetness() != null) {
                boolean isComfortable = isComfortable(data.getTemperature(), data.getWetness());
                String period = data.getRecordTime().toString();

                if (isComfortable) {
                    comfortablePeriods.add(period);
                } else {
                    uncomfortablePeriods.add(period);
                }
            }
        }

        double comfortPercentage = environmentData.isEmpty() ? 0.0 :
                (double) comfortablePeriods.size() / environmentData.size() * 100;

        comfortAssessment.put("comfortablePercentage", Math.round(comfortPercentage * 100.0) / 100.0);
        comfortAssessment.put("comfortablePeriods", comfortablePeriods.size());
        comfortAssessment.put("uncomfortablePeriods", uncomfortablePeriods.size());
        comfortAssessment.put("overallComfort", comfortPercentage >= 80 ? "excellent" :
                                             comfortPercentage >= 60 ? "good" :
                                             comfortPercentage >= 40 ? "fair" : "poor");

        return comfortAssessment;
    }

    /**
     * 判断是否舒适
     */
    private boolean isComfortable(double temperature, double humidity) {
        // 简化的舒适度判断逻辑
        return (temperature >= 20.0 && temperature <= 26.0) &&
               (humidity >= 40.0 && humidity <= 60.0);
    }

    /**
     * 计算极端天气统计
     */
    private Map<String, Object> calculateExtremeWeatherStatistics(List<EnvironmentDeviceStatus> environmentData) {
        Map<String, Object> extremeStats = new HashMap<>();

        int extremelyHotDays = 0;
        int extremelyColdDays = 0;
        int extremelyHumidDays = 0;
        int extremelyDryDays = 0;
        int extremelyWindyDays = 0;

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getTemperature() != null) {
                if (data.getTemperature() > 40.0) extremelyHotDays++;
                if (data.getTemperature() < -15.0) extremelyColdDays++;
            }

            if (data.getWetness() != null) {
                if (data.getWetness() > 90.0) extremelyHumidDays++;
                if (data.getWetness() < 10.0) extremelyDryDays++;
            }

            if (data.getWindSpeed() != null) {
                if (data.getWindSpeed() > 15.0) extremelyWindyDays++;
            }
        }

        extremeStats.put("extremelyHotDays", extremelyHotDays);
        extremeStats.put("extremelyColdDays", extremelyColdDays);
        extremeStats.put("extremelyHumidDays", extremelyHumidDays);
        extremeStats.put("extremelyDryDays", extremelyDryDays);
        extremeStats.put("extremelyWindyDays", extremelyWindyDays);

        return extremeStats;
    }

    /**
     * 计算每小时环境统计
     */
    private Map<Integer, Map<String, Object>> calculateHourlyEnvironmentStatistics(List<EnvironmentDeviceStatus> environmentData) {
        return environmentData.stream()
                .filter(data -> data.getTemperature() != null || data.getWetness() != null || data.getWindSpeed() != null)
                .collect(Collectors.groupingBy(
                        data -> data.getRecordTime().getHour(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                hourData -> Map.of(
                                        "averageTemperature", hourData.stream()
                                                .filter(d -> d.getTemperature() != null)
                                                .mapToDouble(d -> d.getTemperature())
                                                .average().orElse(0.0),
                                        "averageHumidity", hourData.stream()
                                                .filter(d -> d.getWetness() != null)
                                                .mapToDouble(d -> d.getWetness())
                                                .average().orElse(0.0),
                                        "averageWindSpeed", hourData.stream()
                                                .filter(d -> d.getWindSpeed() != null)
                                                .mapToDouble(d -> d.getWindSpeed())
                                                .average().orElse(0.0),
                                        "count", hourData.size()
                                )
                        )
                ));
    }

    /**
     * 计算每日环境统计
     */
    private Map<Integer, Map<String, Object>> calculateDailyEnvironmentStatistics(List<EnvironmentDeviceStatus> environmentData) {
        return environmentData.stream()
                .filter(data -> data.getTemperature() != null || data.getWetness() != null || data.getWindSpeed() != null)
                .collect(Collectors.groupingBy(
                        data -> data.getRecordTime().getDayOfYear(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                dayData -> Map.of(
                                        "averageTemperature", dayData.stream()
                                                .filter(d -> d.getTemperature() != null)
                                                .mapToDouble(d -> d.getTemperature())
                                                .average().orElse(0.0),
                                        "maxTemperature", dayData.stream()
                                                .filter(d -> d.getTemperature() != null)
                                                .mapToDouble(d -> d.getTemperature())
                                                .max().orElse(0.0),
                                        "minTemperature", dayData.stream()
                                                .filter(d -> d.getTemperature() != null)
                                                .mapToDouble(d -> d.getTemperature())
                                                .min().orElse(0.0),
                                        "averageHumidity", dayData.stream()
                                                .filter(d -> d.getWetness() != null)
                                                .mapToDouble(d -> d.getWetness())
                                                .average().orElse(0.0),
                                        "averageWindSpeed", dayData.stream()
                                                .filter(d -> d.getWindSpeed() != null)
                                                .mapToDouble(d -> d.getWindSpeed())
                                                .average().orElse(0.0),
                                        "count", dayData.size()
                                )
                        )
                ));
    }

    /**
     * 检测温度异常
     */
    private List<Map<String, Object>> detectTemperatureAnomalies(List<EnvironmentDeviceStatus> environmentData) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        List<Double> temperatures = environmentData.stream()
                .filter(d -> d.getTemperature() != null)
                .map(d -> d.getTemperature())
                .collect(Collectors.toList());

        if (temperatures.isEmpty()) return anomalies;

        double mean = temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(temperatures, mean);
        double upperThreshold = mean + 3 * stdDev;
        double lowerThreshold = Math.max(0, mean - 3 * stdDev);

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getTemperature() != null) {
                double temp = data.getTemperature();
                if (temp > upperThreshold || temp < lowerThreshold) {
                    anomalies.add(createEnvironmentAnomaly(data.getRecordTime(), "temperature",
                            temp > upperThreshold ? "high" : "low", temp));
                }
            }
        }

        return anomalies;
    }

    /**
     * 检测湿度异常
     */
    private List<Map<String, Object>> detectHumidityAnomalies(List<EnvironmentDeviceStatus> environmentData) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        List<Double> humidity = environmentData.stream()
                .filter(d -> d.getWetness() != null)
                .map(d -> d.getWetness())
                .collect(Collectors.toList());

        if (humidity.isEmpty()) return anomalies;

        double mean = humidity.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(humidity, mean);
        double upperThreshold = mean + 3 * stdDev;
        double lowerThreshold = Math.max(0, mean - 3 * stdDev);

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getWetness() != null) {
                double hum = data.getWetness();
                if (hum > upperThreshold || hum < lowerThreshold) {
                    anomalies.add(createEnvironmentAnomaly(data.getRecordTime(), "wetness",
                            hum > upperThreshold ? "high" : "low", hum));
                }
            }
        }

        return anomalies;
    }

    /**
     * 检测风速异常
     */
    private List<Map<String, Object>> detectWindSpeedAnomalies(List<EnvironmentDeviceStatus> environmentData) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        List<Double> windSpeeds = environmentData.stream()
                .filter(d -> d.getWindSpeed() != null)
                .map(d -> d.getWindSpeed())
                .collect(Collectors.toList());

        if (windSpeeds.isEmpty()) return anomalies;

        double mean = windSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(windSpeeds, mean);
        double upperThreshold = mean + 3 * stdDev;

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getWindSpeed() != null) {
                double wind = data.getWindSpeed();
                if (wind > upperThreshold) {
                    anomalies.add(createEnvironmentAnomaly(data.getRecordTime(), "windSpeed", "high", wind));
                }
            }
        }

        return anomalies;
    }

    /**
     * 检测综合指数异常
     */
    private List<Map<String, Object>> detectTotalIndexAnomalies(List<EnvironmentDeviceStatus> environmentData) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        List<BigDecimal> totalIndexes = environmentData.stream()
                .filter(d -> d.getTotalEnvironmentIndex() != null)
                .map(d -> d.getTotalEnvironmentIndex())
                .collect(Collectors.toList());

        if (totalIndexes.isEmpty()) return anomalies;

        double mean = totalIndexes.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        double stdDev = calculateStandardDeviation(
                totalIndexes.stream().mapToDouble(BigDecimal::doubleValue).collect(Collectors.toList()),
                mean);

        double lowerThreshold = Math.max(0, mean - 3 * stdDev);

        for (EnvironmentDeviceStatus data : environmentData) {
            if (data.getTotalEnvironmentIndex() != null) {
                double index = data.getTotalEnvironmentIndex().doubleValue();
                if (index < lowerThreshold) {
                    anomalies.add(createEnvironmentAnomaly(data.getRecordTime(), "totalIndex", "low", index));
                }
            }
        }

        return anomalies;
    }

    /**
     * 计算标准差
     */
    private double calculateStandardDeviation(List<Double> values, double mean) {
        double sumSquaredDiff = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();

        return Math.sqrt(sumSquaredDiff / values.size());
    }
}