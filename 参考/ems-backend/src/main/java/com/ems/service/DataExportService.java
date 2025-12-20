package com.ems.service;

import com.ems.config.DataExportProperties;
import com.ems.entity.DataExportLog;
import com.ems.repository.DataExportLogRepository;
import com.ems.repository.DeviceStatusRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 月度数据导出服务
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final DataExportProperties properties;
    private final DataExportLogRepository exportLogRepository;
    private final DeviceStatusRecordRepository deviceStatusRecordRepository;
    private final SystemHealthService systemHealthService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    /**
     * 定时月度数据导出任务
     * 每月1日凌晨2点执行
     */
    @Scheduled(cron = "#{@dataExportProperties.cron}")
    @Transactional
    public void scheduledMonthlyExport() {
        if (!properties.isEnabled()) {
            log.info("📦 月度数据导出功能已禁用，跳过执行");
            return;
        }

        log.info("🚀 开始执行定时月度数据导出任务");
        try {
            LocalDate targetMonth = LocalDate.now().minusMonths(properties.getKeepMonths());
            executeExport(targetMonth, false);
        } catch (Exception e) {
            log.error("❌ 定时月度数据导出任务失败", e);
        }
    }

    /**
     * 手动触发数据导出
     *
     * @param targetMonthStr 目标月份字符串 (格式: yyyy-MM)
     * @param dryRun        是否为试运行
     * @param force         是否强制执行
     * @return 导出结果
     */
    @Transactional
    public CompletableFuture<ExportResult> manualExport(String targetMonthStr, boolean dryRun, boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 检查是否有运行中的任务
                if (!force && hasRunningExport()) {
                    return ExportResult.failure("已有导出任务正在执行中");
                }

                LocalDate targetMonth;
                if (targetMonthStr != null && !targetMonthStr.trim().isEmpty()) {
                    targetMonth = YearMonth.parse(targetMonthStr, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
                } else {
                    targetMonth = LocalDate.now().minusMonths(properties.getKeepMonths());
                }

                DataExportLog logEntry = executeExport(targetMonth, dryRun);
                return ExportResult.success(logEntry);

            } catch (Exception e) {
                log.error("❌ 手动触发数据导出失败", e);
                return ExportResult.failure("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 执行数据导出
     *
     * @param targetMonth 目标月份
     * @param dryRun      是否为试运行
     * @return 导出日志
     */
    @Transactional
    public DataExportLog executeExport(LocalDate targetMonth, boolean dryRun) {
        log.info("📦 开始{}数据导出: 目标月份={}", dryRun ? "试运行" : "", targetMonth);

        // 创建导出日志
        DataExportLog exportLog = createExportLog(targetMonth, dryRun);
        exportLogRepository.save(exportLog);

        try {
            // 1. 检查系统健康状态
            checkSystemHealth();

            // 2. 计算导出时间范围
            LocalDateTime monthStart = targetMonth.atStartOfDay();
            LocalDateTime monthEnd = targetMonth.plusMonths(1).atStartOfDay().minusSeconds(1);

            exportLog.setTargetMonthStart(monthStart);
            exportLog.setTargetMonthEnd(monthEnd);
            exportLogRepository.save(exportLog);

            // 3. 统计导出数据量
            Long recordsCount = countRecordsInMonth(monthStart, monthEnd);
            exportLog.setExportedRecordsCount(recordsCount);

            if (recordsCount == null || recordsCount == 0) {
                log.info("📭 目标月份无数据: 月份={}", targetMonth);
                exportLog.setExportStatus(DataExportLog.ExportStatus.SUCCESS);
                exportLog.setErrorMessage("目标月份无数据");
                exportLogRepository.save(exportLog);
                return exportLog;
            }

            // 4. 试运行模式，只统计不执行
            if (dryRun) {
                log.info("🔍 试运行完成: 月份={}, 记录数={}", targetMonth, recordsCount);
                exportLog.setExportStatus(DataExportLog.ExportStatus.SUCCESS);
                exportLog.setErrorMessage("试运行模式，未实际执行导出");
                exportLogRepository.save(exportLog);
                return exportLog;
            }

            // 5. 创建导出目录
            Path exportDir = createExportDirectory(targetMonth);
            String exportFilePath = generateExportFilePath(targetMonth);
            Path exportFile = Paths.get(exportFilePath);

            // 6. 执行导出
            Long fileSize = executeMysqldump(monthStart, monthEnd, exportFile);
            exportLog.setExportFilePath(exportFilePath);
            exportLog.setExportFileSizeBytes(fileSize);
            exportLogRepository.save(exportLog);

            // 7. 验证导出文件
            verifyExportFile(exportFile, recordsCount);

            // 8. 记录导出前记录数
            Long recordsBefore = deviceStatusRecordRepository.count();
            exportLog.setRecordsBeforeExport(recordsBefore);

            // 9. 删除数据库中对应数据
            deleteMonthData(monthStart, monthEnd);

            // 10. 记录导出后记录数
            Long recordsAfter = deviceStatusRecordRepository.count();
            exportLog.setRecordsAfterExport(recordsAfter);

            // 11. 标记成功
            exportLog.setExportStatus(DataExportLog.ExportStatus.SUCCESS);
            long duration = Duration.between(exportLog.getCreatedAt(), LocalDateTime.now()).getSeconds();
            exportLog.setExportDurationSeconds((int) duration);

            log.info("✅ 数据导出成功: 月份={}, 记录数={}, 文件大小={}MB, 耗时={}秒",
                    targetMonth, recordsCount, fileSize / (1024 * 1024), duration);

        } catch (Exception e) {
            log.error("❌ 数据导出失败: 月份={}", targetMonth, e);
            exportLog.setExportStatus(DataExportLog.ExportStatus.FAILED);
            exportLog.setErrorMessage(e.getMessage());
        }

        exportLogRepository.save(exportLog);
        return exportLog;
    }

    /**
     * 创建导出日志
     */
    private DataExportLog createExportLog(LocalDate targetMonth, boolean dryRun) {
        return DataExportLog.builder()
                .exportMonth(targetMonth)
                .exportStatus(DataExportLog.ExportStatus.PENDING)
                .isTestExecution(dryRun)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 检查系统健康状态
     */
    private void checkSystemHealth() {
        // 检查磁盘空间
        long freeSpaceGb = systemHealthService.getFreeDiskSpaceGb(properties.getExportPath());
        if (freeSpaceGb < properties.getMinDiskSpaceGb()) {
            throw new IllegalStateException(String.format(
                    "磁盘空间不足，需要至少%dGB，当前可用%dGB",
                    properties.getMinDiskSpaceGb(), freeSpaceGb));
        }

        // 检查数据库连接
        if (!systemHealthService.isDatabaseHealthy()) {
            throw new IllegalStateException("数据库连接异常，无法执行导出");
        }
    }

    /**
     * 统计指定月份的记录数
     */
    private Long countRecordsInMonth(LocalDateTime startTime, LocalDateTime endTime) {
        return deviceStatusRecordRepository.countByRecordTimeBetween(startTime, endTime);
    }

    /**
     * 创建导出目录
     */
    private Path createExportDirectory(LocalDate month) {
        String yearStr = String.valueOf(month.getYear());
        Path exportDir = Paths.get(properties.getExportPath(), yearStr);

        try {
            Files.createDirectories(exportDir);
            return exportDir;
        } catch (IOException e) {
            throw new RuntimeException("创建导出目录失败: " + exportDir, e);
        }
    }

    /**
     * 生成导出文件路径
     */
    private String generateExportFilePath(LocalDate month) {
        String yearStr = String.valueOf(month.getYear());
        String monthStr = String.format("%02d", month.getMonthValue());
        String fileName = String.format("ems_device_data_%s%s.sql", yearStr, monthStr);
        return Paths.get(properties.getExportPath(), yearStr, fileName).toString();
    }

    /**
     * 执行mysqldump导出
     */
    private Long executeMysqldump(LocalDateTime startTime, LocalDateTime endTime, Path exportFile) {
        log.info("📤 开始执行mysqldump: 目标文件={}", exportFile);

        String dbUrl = getDatabaseUrl();
        String dbName = extractDatabaseName(dbUrl);
        String dbHost = extractDatabaseHost(dbUrl);
        String dbUsername = getDatabaseUsername();
        String dbPassword = getDatabasePassword();

        String timeCondition = String.format("record_time >= '%s' AND record_time <= '%s'",
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        List<String> command = Arrays.asList(
                properties.getMysqldumpPath(),
                "--single-transaction",
                "--quick",
                "--lock-tables=false",
                "--skip-add-locks",
                "--skip-comments",
                "--hex-blob",
                "--default-character-set=utf8mb4",
                "--host=" + dbHost,
                "--user=" + dbUsername,
                String.format("--password=%s", dbPassword),
                String.format("--where=%s", timeCondition),
                dbName,
                properties.getTableName()
        );

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            // 写入到文件
            try (BufferedWriter writer = Files.newBufferedWriter(exportFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                AtomicLong fileSize = new AtomicLong();
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    fileSize.addAndGet(line.length() + 1);
                }

                // 等待进程完成
                boolean completed = process.waitFor(properties.getMaxExportDuration().getSeconds(), java.util.concurrent.TimeUnit.SECONDS);
                int exitCode = completed ? 0 : 1;

                if (exitCode != 0) {
                    String errorMsg = String.format("mysqldump执行失败，退出码=%d", exitCode);
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String errorLine;
                        StringBuilder errorOutput = new StringBuilder();
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorOutput.append(errorLine).append("\n");
                        }
                        errorMsg += ", 错误信息: " + errorOutput.toString();
                    }
                    throw new RuntimeException(errorMsg);
                }

                log.info("📤 mysqldump执行成功: 文件大小={}MB", fileSize.get() / (1024 * 1024));
                return fileSize.get();
            }

        } catch (Exception e) {
            throw new RuntimeException("mysqldump执行失败", e);
        }
    }

    /**
     * 验证导出文件
     */
    private void verifyExportFile(Path exportFile, long expectedRecords) {
        if (!Files.exists(exportFile)) {
            throw new RuntimeException("导出文件不存在: " + exportFile);
        }

        try {
            long fileSize = Files.size(exportFile);
            if (fileSize == 0) {
                throw new RuntimeException("导出文件为空");
            }

            // 验证SQL文件内容完整性
            try (BufferedReader reader = Files.newBufferedReader(exportFile)) {
                String content = reader.lines().limit(100).reduce("", (a, b) -> a + b);
                if (!content.contains("INSERT INTO") || !content.contains(properties.getTableName())) {
                    throw new RuntimeException("导出文件格式异常，未找到预期的INSERT语句");
                }
            }

            log.info("🔍 导出文件验证通过: 大小={}MB", fileSize / (1024 * 1024));

        } catch (IOException e) {
            throw new RuntimeException("导出文件验证失败", e);
        }
    }

    /**
     * 删除指定月份的数据
     */
    @Transactional
    public void deleteMonthData(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("🗑️ 开始删除数据: 时间范围={} ~ {}", startTime, endTime);

        long deletedCount = deviceStatusRecordRepository.deleteByRecordTimeBetween(startTime, endTime);

        log.info("🗑️ 数据删除完成: 删除数量={}", deletedCount);
    }

    /**
     * 检查是否有运行中的导出任务
     */
    private boolean hasRunningExport() {
        return exportLogRepository.countRunningExports() > 0;
    }

    // 数据库配置提取方法
    private String getDatabaseUrl() {
        return datasourceUrl;
    }

    private String extractDatabaseName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private String extractDatabaseHost(String url) {
        return url.substring(url.indexOf("://") + 3, url.indexOf(":"));
    }

    private String getDatabaseUsername() {
        return datasourceUsername;
    }

    private String getDatabasePassword() {
        return datasourcePassword;
    }

    /**
     * 导出结果类
     */
    public static class ExportResult {
        private boolean success;
        private String message;
        private DataExportLog exportLog;

        private ExportResult(boolean success, String message, DataExportLog exportLog) {
            this.success = success;
            this.message = message;
            this.exportLog = exportLog;
        }

        public static ExportResult success(DataExportLog exportLog) {
            return new ExportResult(true, "导出成功", exportLog);
        }

        public static ExportResult failure(String message) {
            return new ExportResult(false, message, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public DataExportLog getExportLog() { return exportLog; }
    }
}