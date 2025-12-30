package com.cdutetc.ems.service;

import com.cdutetc.ems.config.BackupProperties;
import com.cdutetc.ems.entity.BackupLog;
import com.cdutetc.ems.entity.BackupLog.BackupStatus;
import com.cdutetc.ems.entity.BackupLog.BackupType;
import com.cdutetc.ems.repository.BackupLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据备份服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupProperties backupProperties;
    private final BackupLogRepository backupLogRepository;
    private final DataSource dataSource;

    /**
     * 执行时序数据备份
     */
    @Transactional
    public BackupLog backupTimeseriesData(String triggerMode) {
        return backupData(
            BackupType.TIMESERIES,
            "timeseries",
            backupProperties.getTimeseries().getRetentionMonths(),
            backupProperties.getTimeseries().getTables(),
            triggerMode
        );
    }

    /**
     * 执行业务数据备份
     */
    @Transactional
    public BackupLog backupBusinessData(String triggerMode) {
        return backupData(
            BackupType.BUSINESS,
            "business",
            backupProperties.getBusiness().getRetentionMonths(),
            backupProperties.getBusiness().getTables(),
            triggerMode
        );
    }

    /**
     * 执行系统配置数据备份
     */
    @Transactional
    public BackupLog backupSystemData(String triggerMode) {
        return backupSystemDataInternal(triggerMode);
    }

    /**
     * 执行全量备份
     */
    @Transactional
    public List<BackupLog> backupAll(String triggerMode) {
        List<BackupLog> results = new ArrayList<>();

        if (backupProperties.getTimeseries().isEnabled()) {
            results.add(backupTimeseriesData(triggerMode));
        }

        if (backupProperties.getBusiness().isEnabled()) {
            results.add(backupBusinessData(triggerMode));
        }

        if (backupProperties.getSystem().isEnabled()) {
            results.add(backupSystemData(triggerMode));
        }

        return results;
    }

    /**
     * 通用备份方法
     */
    private BackupLog backupData(
        BackupType backupType,
        String subDir,
        int retentionMonths,
        List<String> tables,
        String triggerMode
    ) {
        long startTime = System.currentTimeMillis();

        // 创建备份日志
        BackupLog backupLog = BackupLog.builder()
            .backupType(backupType)
            .status(BackupStatus.RUNNING)
            .triggerMode(triggerMode)
            .retentionMonths(retentionMonths)
            .tables(String.join(",", tables))
            .build();

        backupLog = backupLogRepository.save(backupLog);

        try {
            // 计算截止日期
            LocalDateTime cutoffDateTime = LocalDateTime.now().minusMonths(retentionMonths);
            String cutoffDate = cutoffDateTime.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );

            log.info("开始备份 {} 数据，保留月数: {}, 截止日期: {}",
                backupType, retentionMonths, cutoffDate);

            // 创建备份目录
            String backupDir = backupProperties.getRootDir() + "/" + subDir;
            Files.createDirectories(Paths.get(backupDir));

            // 生成备份文件路径
            String dateStr = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
            );
            String filePrefix = subDir.equals("timeseries") ?
                backupProperties.getTimeseries().getFilePrefix() :
                backupProperties.getBusiness().getFilePrefix();
            String sqlFile = backupDir + "/" + filePrefix + "_" + dateStr + ".sql";

            // 执行备份
            int totalCount = 0;
            int deletedCount = 0;

            for (String table : tables) {
                log.info("备份表: {}", table);

                // 获取记录数
                int count = countRecords(table, cutoffDate);
                totalCount += count;

                // 执行mysqldump
                boolean success = executeMysqldump(table, cutoffDate, sqlFile, false);

                if (!success) {
                    throw new RuntimeException("备份表失败: " + table);
                }

                // 删除旧数据
                deletedCount += deleteOldData(table, cutoffDate);
            }

            // 压缩文件
            File finalFile = new File(sqlFile);
            long fileSize = finalFile.length();

            if (backupProperties.getAdvanced().isCompress()) {
                String compressedFile = compressFile(sqlFile);
                finalFile = new File(compressedFile);
                fileSize = finalFile.length();
                sqlFile = compressedFile;
            }

            // 更新备份日志
            backupLog.setStatus(BackupStatus.SUCCESS);
            backupLog.setFilePath(sqlFile);
            backupLog.setFileSize(fileSize);
            backupLog.setRecordCount(totalCount);
            backupLog.setDeletedCount(deletedCount);
            backupLog.setCutoffDate(cutoffDate);
            backupLog.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("备份成功: {}, 记录数: {}, 文件大小: {} bytes",
                backupType, totalCount, fileSize);

        } catch (Exception e) {
            log.error("备份失败: {}", backupType, e);
            backupLog.setStatus(BackupStatus.FAILED);
            backupLog.setErrorMessage(e.getMessage());
            backupLog.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return backupLogRepository.save(backupLog);
    }

    /**
     * 备份系统数据（包含表结构）
     */
    private BackupLog backupSystemDataInternal(String triggerMode) {
        long startTime = System.currentTimeMillis();

        BackupLog backupLog = BackupLog.builder()
            .backupType(BackupType.SYSTEM)
            .status(BackupStatus.RUNNING)
            .triggerMode(triggerMode)
            .tables(String.join(",", backupProperties.getSystem().getTables()))
            .build();

        backupLog = backupLogRepository.save(backupLog);

        try {
            String backupDir = backupProperties.getRootDir() + "/" +
                backupProperties.getSystem().getSubDir();
            Files.createDirectories(Paths.get(backupDir));

            String dateStr = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
            );
            String sqlFile = backupDir + "/" +
                backupProperties.getSystem().getFilePrefix() + "_" + dateStr + ".sql";

            // 备份系统表（包含表结构）
            for (String table : backupProperties.getSystem().getTables()) {
                executeMysqldump(table, null, sqlFile, true);
            }

            // 压缩文件
            File finalFile = new File(sqlFile);
            long fileSize = finalFile.length();

            if (backupProperties.getAdvanced().isCompress()) {
                String compressedFile = compressFile(sqlFile);
                finalFile = new File(compressedFile);
                fileSize = finalFile.length();
                sqlFile = compressedFile;
            }

            // 清理旧备份
            cleanOldSystemBackups(
                backupDir,
                backupProperties.getSystem().getFilePrefix(),
                backupProperties.getSystem().getKeepCount()
            );

            backupLog.setStatus(BackupStatus.SUCCESS);
            backupLog.setFilePath(sqlFile);
            backupLog.setFileSize(fileSize);
            backupLog.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("系统配置备份成功: {}", sqlFile);

        } catch (Exception e) {
            log.error("系统配置备份失败", e);
            backupLog.setStatus(BackupStatus.FAILED);
            backupLog.setErrorMessage(e.getMessage());
            backupLog.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return backupLogRepository.save(backupLog);
    }

    /**
     * 统计记录数
     */
    private int countRecords(String table, String cutoffDate) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE created_at < '%s'",
            table, cutoffDate
        );

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("统计记录数失败: {}", table, e);
        }
        return 0;
    }

    /**
     * 删除旧数据
     */
    private int deleteOldData(String table, String cutoffDate) {
        String sql = String.format(
            "DELETE FROM %s WHERE created_at < '%s'",
            table, cutoffDate
        );

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            int affected = stmt.executeUpdate(sql);
            log.info("删除表 {} 的旧数据: {} 条", table, affected);
            return affected;

        } catch (Exception e) {
            log.error("删除旧数据失败: {}", table, e);
            return 0;
        }
    }

    /**
     * 执行mysqldump命令
     */
    private boolean executeMysqldump(
        String table,
        String cutoffDate,
        String outputFile,
        boolean includeSchema
    ) throws Exception {

        List<String> command = new ArrayList<>();
        command.add(getMysqldumpPath());
        command.add("-u" + getDbUsername());
        command.add("-p" + getDbPassword());
        command.add("-h" + getDbHost());
        command.add("-P" + getDbPort());
        command.add(getDbName());

        if (includeSchema) {
            command.add(table);
        } else {
            command.add("--no-create-info");
            command.add("--where=created_at < '" + cutoffDate + "'");
            command.add("--single-transaction");
            command.add("--quick");
            command.add("--lock-tables=false");
            command.add(table);
        }

        // 追加到输出文件
        command.add(">>" + outputFile);

        // 执行命令
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 读取输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (backupProperties.getAdvanced().isVerboseLogging()) {
                    log.debug("mysqldump: {}", line);
                }
            }
        }

        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    /**
     * 压缩文件
     */
    private String compressFile(String filePath) throws Exception {
        String compressTool = backupProperties.getAdvanced().getCompressFormat();
        String compressedFile = filePath + "." + compressTool;

        ProcessBuilder pb;
        switch (compressTool) {
            case "gzip":
                pb = new ProcessBuilder("gzip", "-c", filePath);
                break;
            case "bzip2":
                pb = new ProcessBuilder("bzip2", "-k", filePath);
                compressedFile = filePath + ".bz2";
                break;
            case "xz":
                pb = new ProcessBuilder("xz", "-k", filePath);
                compressedFile = filePath + ".xz";
                break;
            default:
                return filePath;
        }

        pb.redirectOutput(new File(compressedFile));
        Process process = pb.start();
        process.waitFor();

        // 删除原文件
        new File(filePath).delete();

        log.info("文件压缩完成: {}", compressedFile);
        return compressedFile;
    }

    /**
     * 清理系统数据的旧备份
     */
    private void cleanOldSystemBackups(
        String backupDir,
        String filePrefix,
        int keepCount
    ) {
        try {
            File dir = new File(backupDir);
            File[] files = dir.listFiles((d, name) ->
                name.startsWith(filePrefix) && (name.endsWith(".sql") ||
                    name.endsWith(".sql.gz") || name.endsWith(".sql.xz"))
            );

            if (files != null && files.length > keepCount) {
                List<File> fileList = new ArrayList<>(List.of(files));
                fileList.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                for (int i = keepCount; i < fileList.size(); i++) {
                    File file = fileList.get(i);
                    if (file.delete()) {
                        log.info("删除旧备份文件: {}", file.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理旧备份文件失败", e);
        }
    }

    /**
     * 获取最近备份记录
     */
    public List<BackupLog> getRecentBackups() {
        return backupLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 获取备份统计信息
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long total = backupLogRepository.count();
        long success = backupLogRepository.countByStatus(BackupStatus.SUCCESS);
        long failed = backupLogRepository.countByStatus(BackupStatus.FAILED);

        stats.put("total", total);
        stats.put("success", success);
        stats.put("failed", failed);
        stats.put("running", backupLogRepository.countByStatus(BackupStatus.RUNNING));

        return stats;
    }

    // 辅助方法
    private String getDbUsername() {
        String username = backupProperties.getDatabase().getUsername();
        return username.isEmpty() ? "ems_user" : username;
    }

    private String getDbPassword() {
        String password = backupProperties.getDatabase().getPassword();
        return password.isEmpty() ? "ems_pass" : password;
    }

    private String getDbHost() {
        String host = backupProperties.getDatabase().getHost();
        return host.isEmpty() ? "localhost" : host;
    }

    private String getDbPort() {
        Integer port = backupProperties.getDatabase().getPort();
        return port != null ? String.valueOf(port) : "3306";
    }

    private String getDbName() {
        String name = backupProperties.getDatabase().getName();
        return name.isEmpty() ? "ems_db" : name;
    }

    private String getMysqldumpPath() {
        String path = backupProperties.getAdvanced().getMysqldumpPath();
        return (path != null && !path.isEmpty()) ? path : "mysqldump";
    }
}
