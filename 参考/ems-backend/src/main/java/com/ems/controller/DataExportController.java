package com.ems.controller;

import com.ems.config.DataExportProperties;
import com.ems.entity.DataExportLog;
import com.ems.repository.DataExportLogRepository;
import com.ems.repository.DeviceStatusRecordRepository;
import com.ems.service.DataExportService;
import com.ems.service.SystemHealthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 数据导出测试控制器
 * 提供手动触发和监控月度数据导出的接口
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/test/data-export")
@RequiredArgsConstructor
@Tag(name = "数据导出测试", description = "数据导出测试相关接口")
public class DataExportController {

    private final DataExportService dataExportService;
    private final DataExportLogRepository exportLogRepository;
    private final DeviceStatusRecordRepository deviceStatusRecordRepository;
    private final DataExportProperties properties;
    private final SystemHealthService systemHealthService;

    /**
     * 手动触发月度数据导出
     *
     * @param targetMonth 目标月份 (格式: yyyy-MM, 可选)
     * @param dryRun 是否为试运行模式 (默认: false)
     * @param force 是否强制执行 (默认: false)
     * @return 执行结果
     */
    @PostMapping("/trigger-monthly-export")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerMonthlyExport(
            @RequestParam(required = false) String targetMonth,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean force) {

        log.info("🧪 [测试接口] 手动触发数据导出: targetMonth={}, dryRun={}, force={}",
                targetMonth, dryRun, force);

        // 验证参数
        if (targetMonth != null) {
            try {
                YearMonth.parse(targetMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (Exception e) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "月份格式错误，请使用 yyyy-MM 格式")));
            }
        }

        return dataExportService.manualExport(targetMonth, dryRun, force)
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", result.isSuccess());
                    response.put("message", result.getMessage());

                    if (result.getExportLog() != null) {
                        response.put("exportLog", convertToMap(result.getExportLog()));
                    }

                    return result.isSuccess() ?
                        ResponseEntity.ok(response) :
                        ResponseEntity.badRequest().body(response);
                })
                .exceptionally(throwable -> {
                    log.error("手动触发导出异常", throwable);
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "导出异常: " + throwable.getMessage()));
                });
    }

    /**
     * 查看导出状态和历史记录
     *
     * @param limit 返回记录数限制 (默认: 20)
     * @param testOnly 是否只查看测试记录 (默认: false)
     * @return 导出状态信息
     */
    @GetMapping("/export-status")
    public ResponseEntity<Map<String, Object>> getExportStatus(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean testOnly) {

        try {
            Map<String, Object> status = new HashMap<>();

            // 当前配置信息
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", properties.isEnabled());
            config.put("cron", properties.getCron());
            config.put("keepMonths", properties.getKeepMonths());
            config.put("exportPath", properties.getExportPath());
            config.put("testTrigger", properties.isTestTrigger());
            status.put("config", config);

            // 运行中的任务
            long runningCount = exportLogRepository.countRunningExports();
            status.put("runningTasks", runningCount);

            // 最近的历史记录
            List<DataExportLog> recentLogs = exportLogRepository.findByIsTestExecutionOrderByCreatedAtDesc(testOnly)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            List<Map<String, Object>> logs = recentLogs.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            status.put("recentLogs", logs);

            // 统计信息
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalExports", exportLogRepository.count());
            statistics.put("successExports", exportLogRepository.countByExportStatus(DataExportLog.ExportStatus.SUCCESS));
            statistics.put("failedExports", exportLogRepository.countByExportStatus(DataExportLog.ExportStatus.FAILED));
            status.put("statistics", statistics);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("获取导出状态失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取数据统计信息
     *
     * @return 数据统计
     */
    @GetMapping("/data-stats")
    public ResponseEntity<Map<String, Object>> getDataStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 当前数据库统计
            long totalRecords = deviceStatusRecordRepository.count();
            stats.put("totalRecords", totalRecords);

            // 各月份数据统计
            Map<String, Long> monthlyStats = new HashMap<>();
            for (int i = 0; i < 12; i++) {
                LocalDate month = LocalDate.now().minusMonths(i);
                LocalDateTime monthStart = month.atStartOfDay();
                LocalDateTime monthEnd = month.plusMonths(1).atStartOfDay().minusSeconds(1);

                Long count = deviceStatusRecordRepository.countByRecordTimeBetween(monthStart, monthEnd);
                if (count != null && count > 0) {
                    monthlyStats.put(month.format(DateTimeFormatter.ofPattern("yyyy-MM")), count);
                }
            }
            stats.put("monthlyStats", monthlyStats);

            // 导出历史统计
            List<Object[]> exportStats = exportLogRepository.getExportStatistics();
            List<Map<String, Object>> exportHistory = exportStats.stream()
                    .map(stat -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("exportMonth", stat[0]);
                        item.put("status", stat[1]);
                        item.put("recordCount", stat[2]);
                        item.put("fileSize", stat[3]);
                        item.put("duration", stat[4]);
                        return item;
                    })
                    .collect(Collectors.toList());
            stats.put("exportHistory", exportHistory);

            // 系统资源信息
            SystemHealthService.SystemLoad systemLoad = systemHealthService.checkSystemLoad();
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("memoryUsagePercent", systemLoad.getMemoryUsagePercent());
            systemInfo.put("diskUsagePercent", systemLoad.getDiskUsagePercent());
            systemInfo.put("freeDiskSpaceGb", systemLoad.getFreeDiskSpaceGb());
            systemInfo.put("databaseHealthy", systemLoad.isDatabaseHealthy());
            systemInfo.put("cpuCores", systemLoad.getCpuCores());
            systemInfo.put("overallHealthy", systemLoad.isHealthy());
            stats.put("systemInfo", systemInfo);

            // 下次导出预估
            LocalDate nextExportMonth = LocalDate.now().minusMonths(properties.getKeepMonths());
            LocalDateTime nextExportStart = nextExportMonth.atStartOfDay();
            LocalDateTime nextExportEnd = nextExportMonth.plusMonths(1).atStartOfDay().minusSeconds(1);

            Long nextExportCount = deviceStatusRecordRepository.countByRecordTimeBetween(nextExportStart, nextExportEnd);
            Map<String, Object> nextExportInfo = new HashMap<>();
            nextExportInfo.put("targetMonth", nextExportMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            nextExportInfo.put("estimatedRecords", nextExportCount);
            nextExportInfo.put("estimatedFileSize", nextExportCount != null ? nextExportCount * 150 : 0); // 估算文件大小(字节)
            stats.put("nextExport", nextExportInfo);

            return ResponseEntity.ok(Map.of("success", true, "data", stats));

        } catch (Exception e) {
            log.error("获取数据统计失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定月份的详细统计
     *
     * @param year 年份
     * @param month 月份
     * @return 详细统计信息
     */
    @GetMapping("/monthly-stats/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyStats(
            @PathVariable int year,
            @PathVariable int month) {

        try {
            LocalDate targetMonth = LocalDate.of(year, month, 1);
            LocalDateTime monthStart = targetMonth.atStartOfDay();
            LocalDateTime monthEnd = targetMonth.plusMonths(1).atStartOfDay().minusSeconds(1);

            Map<String, Object> stats = new HashMap<>();
            stats.put("year", year);
            stats.put("month", month);
            stats.put("monthName", targetMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")));

            // 数据统计
            Long recordCount = deviceStatusRecordRepository.countByRecordTimeBetween(monthStart, monthEnd);
            stats.put("recordCount", recordCount);

            // 导出历史
            Optional<DataExportLog> exportLog = exportLogRepository.findByExportMonth(targetMonth);
            exportLog.ifPresent(log -> stats.put("exportInfo", convertToMap(log)));

            return ResponseEntity.ok(Map.of("success", true, "data", stats));

        } catch (Exception e) {
            log.error("获取月度统计失败: year={}, month={}", year, month, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "获取月度统计失败: " + e.getMessage()));
        }
    }

    /**
     * 切换测试模式开关
     *
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PostMapping("/toggle-test-mode")
    public ResponseEntity<Map<String, Object>> toggleTestMode(@RequestParam boolean enabled) {
        try {
            properties.setTestTrigger(enabled);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "测试模式已" + (enabled ? "启用" : "禁用"),
                "testTrigger", enabled
            ));

        } catch (Exception e) {
            log.error("切换测试模式失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "切换失败: " + e.getMessage()));
        }
    }

    /**
     * 清理测试数据（仅限测试记录）
     *
     * @return 清理结果
     */
    @DeleteMapping("/cleanup-test-data")
    public ResponseEntity<Map<String, Object>> cleanupTestData() {
        try {
            List<DataExportLog> testLogs = exportLogRepository.findByIsTestExecutionOrderByCreatedAtDesc(true);

            // 只删除状态为成功的测试记录，保留失败的记录以便分析
            long deletedCount = testLogs.stream()
                    .filter(log -> DataExportLog.ExportStatus.SUCCESS.equals(log.getExportStatus()))
                    .count();

            List<DataExportLog> logsToDelete = testLogs.stream()
                    .filter(log -> DataExportLog.ExportStatus.SUCCESS.equals(log.getExportStatus()))
                    .collect(Collectors.toList());
            exportLogRepository.deleteAll(logsToDelete);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("已清理 %d 条测试记录", deletedCount),
                "deletedCount", deletedCount
            ));

        } catch (Exception e) {
            log.error("清理测试数据失败", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "清理失败: " + e.getMessage()));
        }
    }

    /**
     * 将导出日志转换为Map
     */
    private Map<String, Object> convertToMap(DataExportLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("exportMonth", log.getExportMonth());
        map.put("targetMonthStart", log.getTargetMonthStart());
        map.put("targetMonthEnd", log.getTargetMonthEnd());
        map.put("exportFilePath", log.getExportFilePath());
        map.put("recordsBeforeExport", log.getRecordsBeforeExport());
        map.put("recordsAfterExport", log.getRecordsAfterExport());
        map.put("exportFileSizeBytes", log.getExportFileSizeBytes());
        map.put("exportFileSizeGb", log.getExportFileSizeGb());
        map.put("exportedRecordsCount", log.getExportedRecordsCount());
        map.put("deletedRecordsCount", log.getDeletedRecordsCount());
        map.put("exportStatus", log.getExportStatus());
        map.put("exportDurationSeconds", log.getExportDurationSeconds());
        map.put("errorMessage", log.getErrorMessage());
        map.put("isTestExecution", log.getIsTestExecution());
        map.put("createdAt", log.getCreatedAt());
        map.put("completedAt", log.getCompletedAt());
        return map;
    }
}