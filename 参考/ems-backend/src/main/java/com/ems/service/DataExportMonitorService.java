package com.ems.service;

import com.ems.config.DataExportProperties;
import com.ems.entity.DataExportLog;
import com.ems.repository.DataExportLogRepository;
import com.ems.repository.DeviceStatusRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据导出监控服务
 * 负责健康检查、告警和趋势分析
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportMonitorService {

    private final DataExportLogRepository exportLogRepository;
    private final DeviceStatusRecordRepository deviceStatusRecordRepository;
    private final SystemHealthService systemHealthService;
    private final DataExportProperties properties;

    // 监控指标缓存
    private final Map<String, Object> monitorCache = new ConcurrentHashMap<>();
    private LocalDateTime lastCacheUpdate = LocalDateTime.now().minusMinutes(10);

    /**
     * 每日健康检查任务
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyHealthCheck() {
        log.info("🔍 开始执行每日数据导出健康检查");
        try {
            performDailyHealthCheck();
            log.info("✅ 每日健康检查完成");
        } catch (Exception e) {
            log.error("❌ 每日健康检查失败", e);
            sendAlert("每日健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 每周分析报告
     * 每周一凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void weeklyAnalysisReport() {
        log.info("📊 开始生成每周数据分析报告");
        try {
            generateWeeklyReport();
            log.info("✅ 周报生成完成");
        } catch (Exception e) {
            log.error("❌ 周报生成失败", e);
        }
    }

    /**
     * 监控告警检查（每小时执行）
     */
    @Scheduled(fixedDelay = 3600000) // 每小时执行一次
    public void monitorAlertsCheck() {
        try {
            checkForAlerts();
        } catch (Exception e) {
            log.error("监控告警检查异常", e);
        }
    }

    /**
     * 执行每日健康检查
     */
    private void performDailyHealthCheck() {
        Map<String, Object> report = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 1. 检查数据库记录数量趋势
        long totalRecords = deviceStatusRecordRepository.count();
        report.put("totalRecords", totalRecords);
        report.put("checkDate", today);

        // 2. 检查各月份数据量
        Map<String, Long> monthlyDataCount = new HashMap<>();
        for (int i = 0; i < 13; i++) { // 检查13个月的数据
            LocalDate month = today.minusMonths(i);
            LocalDateTime monthStart = month.atStartOfDay();
            LocalDateTime monthEnd = month.plusMonths(1).atStartOfDay().minusSeconds(1);

            Long count = deviceStatusRecordRepository.countByRecordTimeBetween(monthStart, monthEnd);
            if (count != null && count > 0) {
                monthlyDataCount.put(month.format(DateTimeFormatter.ofPattern("yyyy-MM")), count);
            }
        }
        report.put("monthlyDataCount", monthlyDataCount);

        // 3. 检查最近的导出状态
        List<DataExportLog> recentExports = exportLogRepository.findRecentSuccessfulExports(
            org.springframework.data.domain.PageRequest.of(0, 5));
        report.put("recentSuccessfulExports", recentExports.size());

        // 4. 检查失败的导出
        List<DataExportLog> failedExports = exportLogRepository.findByExportStatus(
            DataExportLog.ExportStatus.FAILED);
        report.put("failedExportsCount", failedExports.size());

        if (failedExports.size() > 3) {
            sendAlert("最近失败导出次数过多: " + failedExports.size());
        }

        // 5. 检查磁盘空间
        long freeSpaceGb = systemHealthService.getFreeDiskSpaceGb(properties.getExportPath());
        report.put("freeDiskSpaceGb", freeSpaceGb);

        if (freeSpaceGb < properties.getMinDiskSpaceGb()) {
            sendAlert("导出目录磁盘空间不足: " + freeSpaceGb + "GB");
        }

        // 6. 检查数据增长趋势
        checkDataGrowthTrend(monthlyDataCount, report);

        // 更新缓存
        monitorCache.put("dailyReport", report);
        monitorCache.put("lastDailyCheck", LocalDateTime.now());

        log.info("🔍 每日健康检查结果: 总记录数={}, 磁盘空间={}GB, 失败导出数={}",
                totalRecords, freeSpaceGb, failedExports.size());
    }

    /**
     * 检查数据增长趋势
     */
    private void checkDataGrowthTrend(Map<String, Long> monthlyDataCount, Map<String, Object> report) {
        if (monthlyDataCount.size() < 3) {
            return; // 数据不足，无法分析趋势
        }

        List<String> sortedMonths = new ArrayList<>(monthlyDataCount.keySet());
        sortedMonths.sort(String::compareTo);

        // 获取最近3个月的数据
        String currentMonth = sortedMonths.get(sortedMonths.size() - 1);
        String lastMonth = sortedMonths.get(sortedMonths.size() - 2);
        String lastTwoMonth = sortedMonths.get(sortedMonths.size() - 3);

        Long currentCount = monthlyDataCount.get(currentMonth);
        Long lastCount = monthlyDataCount.get(lastMonth);
        Long lastTwoCount = monthlyDataCount.get(lastTwoMonth);

        if (currentCount != null && lastCount != null && lastTwoCount != null) {
            // 计算增长率
            double growthRate1 = (double) (currentCount - lastCount) / lastCount * 100;
            double growthRate2 = (double) (lastCount - lastTwoCount) / lastTwoCount * 100;

            report.put("growthRate", Math.round(growthRate1));
            report.put("lastGrowthRate", Math.round(growthRate2));

            // 检查异常增长或下降
            if (Math.abs(growthRate1 - growthRate2) > 50) {
                sendAlert(String.format("数据量变化异常: 本月增长率%.1f%%, 上月增长率%.1f%%",
                        growthRate1, growthRate2));
            }

            // 检查数据量异常减少
            if (growthRate1 < -30) {
                sendAlert(String.format("数据量异常减少: 本月数据量%d, 上月数据%d, 减少%.1f%%",
                        currentCount, lastCount, -growthRate1));
            }
        }
    }

    /**
     * 检查告警条件
     */
    private void checkForAlerts() {
        // 检查是否有长时间运行的任务
        long runningTasks = exportLogRepository.countRunningExports();
        if (runningTasks > 0) {
            // 查找长时间运行的任务（超过2小时）
            LocalDateTime threshold = LocalDateTime.now().minusHours(2);
            List<DataExportLog> longRunningTasks = exportLogRepository.findByExportStatusAndCreatedAtBefore(
                DataExportLog.ExportStatus.RUNNING, threshold);

            if (!longRunningTasks.isEmpty()) {
                sendAlert("存在长时间运行的导出任务: " + longRunningTasks.size() + "个任务");
            }
        }

        // 检查磁盘空间
        long freeSpaceGb = systemHealthService.getFreeDiskSpaceGb("/");
        if (freeSpaceGb < 5) {
            sendAlert("系统磁盘空间严重不足: " + freeSpaceGb + "GB");
        }

        // 检查系统资源
        SystemHealthService.SystemLoad systemLoad = systemHealthService.checkSystemLoad();
        if (!systemLoad.isHealthy()) {
            sendAlert("系统资源异常: 内存使用" + Math.round(systemLoad.getMemoryUsagePercent()) + "%" +
                    ", 磁盘使用" + Math.round(systemLoad.getDiskUsagePercent()) + "%");
        }
    }

    /**
     * 生成周报
     */
    private void generateWeeklyReport() {
        LocalDate weekStart = LocalDate.now().minusDays(7);
        LocalDate weekEnd = LocalDate.now();
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.atTime(23, 59, 59);

        Map<String, Object> weeklyReport = new HashMap<>();
        weeklyReport.put("weekStart", weekStart);
        weeklyReport.put("weekEnd", weekEnd);

        // 本周新增数据
        Long weekNewRecords = deviceStatusRecordRepository.countByRecordTimeBetween(weekStartTime, weekEndTime);
        weeklyReport.put("weekNewRecords", weekNewRecords);

        // 本周导出记录
        List<DataExportLog> weekExports = exportLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
            weekStartTime.atOffset(ZoneOffset.UTC).toLocalDateTime(),
            weekEndTime.atOffset(ZoneOffset.UTC).toLocalDateTime());
        weeklyReport.put("weekExports", weekExports.size());

        // 统计导出结果
        long successExports = weekExports.stream()
                .filter(log -> DataExportLog.ExportStatus.SUCCESS.equals(log.getExportStatus()))
                .count();
        long failedExports = weekExports.stream()
                .filter(log -> DataExportLog.ExportStatus.FAILED.equals(log.getExportStatus()))
                .count();

        weeklyReport.put("successExports", successExports);
        weeklyReport.put("failedExports", failedExports);

        // 计算平均数据量
        if (!weekExports.isEmpty()) {
            double avgRecords = weekExports.stream()
                    .filter(log -> log.getExportedRecordsCount() != null)
                    .mapToLong(DataExportLog::getExportedRecordsCount)
                    .average()
                    .orElse(0.0);
            weeklyReport.put("avgExportRecords", Math.round(avgRecords));
        }

        // 存储周报
        monitorCache.put("weeklyReport", weeklyReport);
        monitorCache.put("lastWeeklyReport", LocalDateTime.now());

        log.info("📊 周报: 新增记录={}, 成功导出={}, 失败导出={}",
                weekNewRecords, successExports, failedExports);
    }

    /**
     * 获取监控数据
     */
    public Map<String, Object> getMonitorData() {
        Map<String, Object> data = new HashMap<>();

        // 基本统计
        data.put("totalRecords", deviceStatusRecordRepository.count());
        data.put("totalExports", exportLogRepository.count());
        data.put("runningTasks", exportLogRepository.countRunningExports());

        // 最近24小时的导出
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long recentExports = exportLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
            yesterday, LocalDateTime.now()).size();
        data.put("recentExports", recentExports);

        // 系统资源
        SystemHealthService.SystemLoad systemLoad = systemHealthService.checkSystemLoad();
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("memoryUsage", Math.round(systemLoad.getMemoryUsagePercent()));
        systemInfo.put("diskUsage", Math.round(systemLoad.getDiskUsagePercent()));
        systemInfo.put("freeDiskSpace", systemLoad.getFreeDiskSpaceGb());
        systemInfo.put("databaseHealthy", systemLoad.isDatabaseHealthy());
        systemInfo.put("overall", systemLoad.isHealthy());
        data.put("systemInfo", systemInfo);

        // 缓存的报告数据
        data.putAll(monitorCache);

        return data;
    }

    /**
     * 获取月度数据趋势（最近12个月）
     */
    public Map<String, Object> getMonthlyTrend() {
        Map<String, Object> trend = new HashMap<>();
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> monthlyData = new ArrayList<>();
        long cumulativeRecords = 0;

        for (int i = 11; i >= 0; i--) { // 最近12个月
            LocalDate month = today.minusMonths(i);
            LocalDateTime monthStart = month.atStartOfDay();
            LocalDateTime monthEnd = month.plusMonths(1).atStartOfDay().minusSeconds(1);

            Long monthRecords = deviceStatusRecordRepository.countByRecordTimeBetween(monthStart, monthEnd);
            cumulativeRecords += monthRecords != null ? monthRecords : 0;

            // 检查该月是否有导出记录
            Optional<DataExportLog> exportLog = exportLogRepository.findByExportMonth(month);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            monthData.put("monthName", month.format(DateTimeFormatter.ofPattern("yyyy年MM月")));
            monthData.put("recordCount", monthRecords != null ? monthRecords : 0);
            monthData.put("cumulativeRecords", cumulativeRecords);
            monthData.put("exported", exportLog.isPresent());
            monthData.put("exportStatus", exportLog.map(log -> log.getExportStatus().toString()).orElse("NONE"));
            monthData.put("exportFileSize", exportLog.map(DataExportLog::getExportFileSizeGb).orElse(null));

            monthlyData.add(monthData);
        }

        trend.put("monthlyData", monthlyData);
        trend.put("totalMonths", monthlyData.size());
        trend.put("lastUpdate", LocalDateTime.now());

        return trend;
    }

    /**
     * 发送告警
     */
    private void sendAlert(String message) {
        log.warn("🚨 [告警] {}", message);

        // 这里可以扩展为发送邮件、短信、Slack等
        // 目前只记录日志，后续可以添加具体的通知方式

        // 记录告警到缓存
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) monitorCache.computeIfAbsent(
                "alerts", k -> new ArrayList<>());

        Map<String, Object> alert = new HashMap<>();
        alert.put("message", message);
        alert.put("timestamp", LocalDateTime.now());
        alert.put("level", "WARNING");
        alerts.add(alert);

        // 保留最近100条告警
        if (alerts.size() > 100) {
            alerts.subList(0, alerts.size() - 100).clear();
        }

        monitorCache.put("lastAlert", LocalDateTime.now());
    }

    /**
     * 获取告警列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAlerts() {
        return (List<Map<String, Object>>) monitorCache.getOrDefault("alerts", new ArrayList<>());
    }

    /**
     * 清理告警列表
     */
    public void clearAlerts() {
        monitorCache.remove("alerts");
    }
}