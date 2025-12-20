package com.ems.service.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 图表数据路由服务
 * 重构后统一使用实时数据查询，不再依赖聚合表
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChartDataRoutingService {

    private final RealtimeDataQueryService realtimeDataQueryService;

    /**
     * 智能路由查询图表数据（统一使用实时数据）
     *
     * @param deviceId 设备ID
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 图表数据点列表
     */
    public List<ChartDataPoint> routeChartDataQuery(String deviceId, String metricName,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        long daysBetween = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate());

        log.debug("📊 图表数据查询: 设备={}, 指标={}, 时间跨度={}天", deviceId, metricName, daysBetween);

        try {
            // 统一使用实时数据查询服务
            return realtimeDataQueryService.queryRealtimeData(deviceId, metricName, startTime, endTime);
        } catch (Exception e) {
            log.error("❌ 图表数据查询失败: 设备={}, 指标={}", deviceId, metricName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取推荐的图表数据粒度
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 推荐的数据粒度
     */
    public DataGranularity recommendGranularity(LocalDateTime startTime, LocalDateTime endTime) {
        String granularity = realtimeDataQueryService.getRecommendedGranularity(startTime, endTime);

        // 根据粒度返回对应的枚举
        switch (granularity) {
            case "30分钟":
                return DataGranularity.MINUTES_30;
            case "1小时":
                return DataGranularity.HOURLY;
            case "6小时":
                return DataGranularity.SIX_HOURLY;
            case "24小时":
                return DataGranularity.DAILY;
            case "7天":
                return DataGranularity.WEEKLY;
            default:
                return DataGranularity.HOURLY;
        }
    }

    /**
     * 图表数据点
     */
    public static class ChartDataPoint {
        private LocalDateTime timestamp;
        private double value;
        private int count;

        public ChartDataPoint(LocalDateTime timestamp, double value, int count) {
            this.timestamp = timestamp;
            this.value = value;
            this.count = count;
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getValue() { return value; }
        public int getCount() { return count; }
    }

    /**
     * 数据粒度枚举
     */
    public enum DataGranularity {
        MINUTES_30("30分钟", 30),
        HOURLY("小时", 60),
        SIX_HOURLY("6小时", 360),
        DAILY("日", 1440),
        WEEKLY("周", 10080);

        private final String description;
        private final int minutes;

        DataGranularity(String description, int minutes) {
            this.description = description;
            this.minutes = minutes;
        }

        public String getDescription() { return description; }
        public int getMinutes() { return minutes; }
    }
}