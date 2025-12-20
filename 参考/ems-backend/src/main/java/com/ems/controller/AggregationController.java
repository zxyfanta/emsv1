package com.ems.controller.device;

import com.ems.dto.common.ApiResponse;
import com.ems.service.aggregation.ChartDataRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表数据查询控制器
 * 重构后仅提供图表数据查询功能，不再包含聚合相关接口
 */
@Slf4j
@RestController
@RequestMapping("/aggregation")
@RequiredArgsConstructor
@Tag(name = "图表数据查询", description = "图表数据查询相关接口")
public class AggregationController {

    private final ChartDataRoutingService chartDataRoutingService;

    /**
     * 智能路由图表数据查询
     * 根据时间跨度自动选择最合适的数据采样策略
     */
    @GetMapping("/charts/metrics")
    @Operation(summary = "获取图表数据", description = "根据时间跨度智能选择数据采样策略返回图表数据")
    public ResponseEntity<ApiResponse<List<ChartDataRoutingService.ChartDataPoint>>> getChartData(
            @Parameter(description = "设备ID", required = true) @RequestParam String deviceId,
            @Parameter(description = "指标名称", required = true) @RequestParam String metricName,
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "开始时间不能晚于结束时间"));
            }

            List<ChartDataRoutingService.ChartDataPoint> data = chartDataRoutingService
                .routeChartDataQuery(deviceId, metricName, startTime, endTime);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("获取图表数据失败: 设备={}, 指标={}", deviceId, metricName, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取图表数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取推荐的数据粒度
     */
    @GetMapping("/granularity/recommend")
    @Operation(summary = "获取推荐数据粒度", description = "根据时间跨度返回推荐的数据粒度")
    public ResponseEntity<ApiResponse<ChartDataRoutingService.DataGranularity>> getRecommendedGranularity(
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "开始时间不能晚于结束时间"));
            }

            ChartDataRoutingService.DataGranularity granularity = chartDataRoutingService
                .recommendGranularity(startTime, endTime);

            return ResponseEntity.ok(ApiResponse.success(granularity));

        } catch (Exception e) {
            log.error("获取推荐数据粒度失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取推荐数据粒度失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取系统状态", description = "获取当前查询系统的状态信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // 系统状态
            status.put("status", "ACTIVE");
            status.put("description", "系统已重构为统一使用实时数据查询");
            status.put("dataStrategy", "REALTIME_ONLY");
            status.put("currentTime", LocalDateTime.now());

            // 数据采样策略说明
            Map<String, String> samplingStrategies = new HashMap<>();
            samplingStrategies.put("1天内", "30分钟采样");
            samplingStrategies.put("7天内", "1小时采样");
            samplingStrategies.put("30天内", "6小时采样");
            samplingStrategies.put("365天内", "24小时采样");
            samplingStrategies.put("超过365天", "7天采样");
            status.put("samplingStrategies", samplingStrategies);

            return ResponseEntity.ok(ApiResponse.success(status));

        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取系统状态失败: " + e.getMessage()));
        }
    }
}