package com.ems.controller.device;

import com.ems.service.DataExportMonitorService;
import com.ems.service.SystemHealthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导出监控控制器
 * 提供监控数据和告警管理的API接口
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/monitor/data-export")
@RequiredArgsConstructor
@Tag(name = "数据导出监控", description = "数据导出监控相关接口")
public class DataExportMonitorController {

    private final DataExportMonitorService monitorService;
    private final SystemHealthService systemHealthService;

    /**
     * 获取监控概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> data = monitorService.getMonitorData();
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("获取监控概览失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取监控数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取月度数据趋势
     */
    @GetMapping("/monthly-trend")
    public ResponseEntity<Map<String, Object>> getMonthlyTrend() {
        try {
            Map<String, Object> trend = monitorService.getMonthlyTrend();
            return ResponseEntity.ok(Map.of("success", true, "data", trend));
        } catch (Exception e) {
            log.error("获取月度趋势失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取月度趋势失败: " + e.getMessage()));
        }
    }

    /**
     * 获取告警列表
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        try {
            List<Map<String, Object>> alerts = monitorService.getAlerts();
            return ResponseEntity.ok(Map.of("success", true, "data", alerts));
        } catch (Exception e) {
            log.error("获取告警列表失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取告警列表失败: " + e.getMessage()));
        }
    }

    /**
     * 清理告警列表
     */
    @DeleteMapping("/alerts")
    public ResponseEntity<Map<String, Object>> clearAlerts() {
        try {
            monitorService.clearAlerts();
            return ResponseEntity.ok(Map.of("success", true, "message", "告警列表已清理"));
        } catch (Exception e) {
            log.error("清理告警列表失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "清理告警列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/system-health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            SystemHealthService.SystemLoad systemLoad = systemHealthService.checkSystemLoad();
            Map<String, Object> health = new HashMap<>();

            health.put("overall", systemLoad.isHealthy());
            health.put("memoryUsage", Math.round(systemLoad.getMemoryUsagePercent()));
            health.put("diskUsage", Math.round(systemLoad.getDiskUsagePercent()));
            health.put("freeDiskSpace", systemLoad.getFreeDiskSpaceGb());
            health.put("databaseHealthy", systemLoad.isDatabaseHealthy());
            health.put("cpuCores", systemLoad.getCpuCores());
            health.put("timestamp", java.time.LocalDateTime.now());

            // 健康状态评估
            String status = "HEALTHY";
            if (systemLoad.getMemoryUsagePercent() > 85 || systemLoad.getDiskUsagePercent() > 90) {
                status = "WARNING";
            }
            if (!systemLoad.isDatabaseHealthy() || systemLoad.getFreeDiskSpaceGb() < 5) {
                status = "CRITICAL";
            }
            health.put("status", status);

            return ResponseEntity.ok(Map.of("success", true, "data", health));
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取系统健康状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取详细的系统资源信息
     */
    @GetMapping("/system-resources")
    public ResponseEntity<Map<String, Object>> getSystemResources() {
        try {
            Map<String, Object> resources = systemHealthService.getSystemResourceInfo();
            return ResponseEntity.ok(Map.of("success", true, "data", resources));
        } catch (Exception e) {
            log.error("获取系统资源信息失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取系统资源信息失败: " + e.getMessage()));
        }
    }

    /**
     * 检查指定路径的磁盘使用情况
     *
     * @param path 文件路径
     */
    @GetMapping("/disk-usage")
    public ResponseEntity<Map<String, Object>> getDiskUsage(@RequestParam(required = false, defaultValue = "/") String path) {
        try {
            Map<String, Object> usage = new HashMap<>();

            long totalSpace = systemHealthService.getTotalDiskSpaceGb(path);
            long freeSpace = systemHealthService.getFreeDiskSpaceGb(path);
            double usagePercent = systemHealthService.getDiskUsagePercent(path);
            boolean isWritable = systemHealthService.isDirectoryWritable(path);

            usage.put("path", path);
            usage.put("totalSpaceGb", totalSpace);
            usage.put("freeSpaceGb", freeSpace);
            usage.put("usedSpaceGb", totalSpace - freeSpace);
            usage.put("usagePercent", Math.round(usagePercent));
            usage.put("writable", isWritable);
            usage.put("timestamp", java.time.LocalDateTime.now());

            // 状态评估
            String status = "OK";
            if (usagePercent > 90) {
                status = "CRITICAL";
            } else if (usagePercent > 80) {
                status = "WARNING";
            }
            usage.put("status", status);

            return ResponseEntity.ok(Map.of("success", true, "data", usage));
        } catch (Exception e) {
            log.error("获取磁盘使用情况失败: path={}", path, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取磁盘使用情况失败: " + e.getMessage()));
        }
    }

    /**
     * 触发手动健康检查
     */
    @PostMapping("/health-check")
    public ResponseEntity<Map<String, Object>> triggerHealthCheck() {
        try {
            // 强制执行健康检查（通过重新创建监控服务）
            Map<String, Object> healthResult = monitorService.getMonitorData();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "手动健康检查已完成",
                "data", healthResult
            ));
        } catch (Exception e) {
            log.error("手动健康检查失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "手动健康检查失败: " + e.getMessage()));
        }
    }

    /**
     * 获取监控告警的配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMonitorConfig() {
        try {
            Map<String, Object> config = new HashMap<>();

            // 监控任务配置
            Map<String, Object> schedule = new HashMap<>();
            schedule.put("dailyHealthCheck", "0 0 1 * * ?"); // 每天凌晨1点
            schedule.put("weeklyAnalysis", "0 0 3 * * MON"); // 每周一凌晨3点
            schedule.put("alertCheck", "3600000ms"); // 每小时
            config.put("schedule", schedule);

            // 告警阈值
            Map<String, Object> thresholds = new HashMap<>();
            thresholds.put("diskSpaceCritical", 5); // GB
            thresholds.put("diskSpaceWarning", 10); // GB
            thresholds.put("memoryUsageWarning", 85); // %
            thresholds.put("memoryUsageCritical", 95); // %
            thresholds.put("diskUsageWarning", 80); // %
            thresholds.put("diskUsageCritical", 90); // %
            config.put("thresholds", thresholds);

            // 监控指标
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("maxAlertsStored", 100); // 最大存储告警数
            metrics.put("dailyReportRetention", 7); // 天
            metrics.put("weeklyReportRetention", 12); // 周
            config.put("metrics", metrics);

            return ResponseEntity.ok(Map.of("success", true, "data", config));
        } catch (Exception e) {
            log.error("获取监控配置失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取监控配置失败: " + e.getMessage()));
        }
    }
}