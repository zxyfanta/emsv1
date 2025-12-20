package com.ems.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康监控服务
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class SystemHealthService implements HealthIndicator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Spring Actuator健康检查
     */
    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 检查数据库连接
            boolean dbHealthy = isDatabaseHealthy();
            details.put("database", dbHealthy ? "UP" : "DOWN");

            // 检查磁盘空间
            long freeSpaceGb = getFreeDiskSpaceGb("/");
            details.put("freeDiskSpaceGb", freeSpaceGb);
            details.put("diskHealthy", freeSpaceGb > 5); // 至少5GB空闲

            // 检查内存使用
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

            details.put("totalMemoryMb", totalMemory / 1024 / 1024);
            details.put("usedMemoryMb", usedMemory / 1024 / 1024);
            details.put("memoryUsagePercent", Math.round(memoryUsagePercent));
            details.put("memoryHealthy", memoryUsagePercent < 85); // 内存使用率低于85%

            // 总体健康状态
            boolean overallHealthy = dbHealthy && freeSpaceGb > 5 && memoryUsagePercent < 85;

            return overallHealthy ?
                Health.up().withDetails(details).build() :
                Health.down().withDetails(details).build();

        } catch (Exception e) {
            log.error("系统健康检查失败", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * 检查数据库健康状态
     */
    public boolean isDatabaseHealthy() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取指定路径的可用磁盘空间(GB)
     */
    public long getFreeDiskSpaceGb(String path) {
        try {
            FileStore store = Files.getFileStore(Paths.get(path));
            long freeBytes = store.getUsableSpace();
            return freeBytes / 1024 / 1024 / 1024;
        } catch (Exception e) {
            log.error("获取磁盘空间失败: path={}", path, e);
            return 0;
        }
    }

    /**
     * 获取路径的总磁盘空间(GB)
     */
    public long getTotalDiskSpaceGb(String path) {
        try {
            FileStore store = Files.getFileStore(Paths.get(path));
            long totalBytes = store.getTotalSpace();
            return totalBytes / 1024 / 1024 / 1024;
        } catch (Exception e) {
            log.error("获取总磁盘空间失败: path={}", path, e);
            return 0;
        }
    }

    /**
     * 获取磁盘空间使用率(%)
     */
    public double getDiskUsagePercent(String path) {
        try {
            FileStore store = Files.getFileStore(Paths.get(path));
            long totalBytes = store.getTotalSpace();
            long freeBytes = store.getUsableSpace();
            long usedBytes = totalBytes - freeBytes;
            return (double) usedBytes / totalBytes * 100;
        } catch (Exception e) {
            log.error("获取磁盘使用率失败: path={}", path, e);
            return 0;
        }
    }

    /**
     * 检查目录是否存在且有写权限
     */
    public boolean isDirectoryWritable(String path) {
        File directory = new File(path);

        if (!directory.exists()) {
            try {
                return directory.mkdirs();
            } catch (Exception e) {
                log.error("创建目录失败: path={}", path, e);
                return false;
            }
        }

        if (!directory.isDirectory()) {
            return false;
        }

        return directory.canWrite();
    }

    /**
     * 获取系统资源使用情况
     */
    public Map<String, Object> getSystemResourceInfo() {
        Map<String, Object> info = new HashMap<>();

        // CPU信息
        Runtime runtime = Runtime.getRuntime();
        int processors = runtime.availableProcessors();
        info.put("cpuCores", processors);

        // 内存信息
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        info.put("totalMemoryMb", totalMemory / 1024 / 1024);
        info.put("usedMemoryMb", usedMemory / 1024 / 1024);
        info.put("freeMemoryMb", freeMemory / 1024 / 1024);
        info.put("maxMemoryMb", maxMemory / 1024 / 1024);
        info.put("memoryUsagePercent", Math.round((double) usedMemory / maxMemory * 100));

        // 磁盘信息
        String[] paths = {"/", "/tmp", "/data"};
        Map<String, Map<String, Object>> diskInfo = new HashMap<>();

        for (String path : paths) {
            Map<String, Object> pathInfo = new HashMap<>();
            try {
                FileStore store = Files.getFileStore(Paths.get(path));
                long totalBytes = store.getTotalSpace();
                long freeBytes = store.getUsableSpace();
                long usedBytes = totalBytes - freeBytes;

                pathInfo.put("totalGb", totalBytes / 1024 / 1024 / 1024);
                pathInfo.put("freeGb", freeBytes / 1024 / 1024 / 1024);
                pathInfo.put("usedGb", usedBytes / 1024 / 1024 / 1024);
                pathInfo.put("usagePercent", Math.round((double) usedBytes / totalBytes * 100));
                pathInfo.put("writable", isDirectoryWritable(path));

                diskInfo.put(path, pathInfo);
            } catch (Exception e) {
                pathInfo.put("error", e.getMessage());
                diskInfo.put(path, pathInfo);
            }
        }
        info.put("diskInfo", diskInfo);

        return info;
    }

    /**
     * 检查系统负载
     */
    public SystemLoad checkSystemLoad() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory * 100;

            long diskSpaceGb = getFreeDiskSpaceGb("/");
            double diskUsage = getDiskUsagePercent("/");

            boolean dbHealthy = isDatabaseHealthy();

            return new SystemLoad(
                memoryUsage,
                diskUsage,
                diskSpaceGb,
                dbHealthy,
                runtime.availableProcessors()
            );

        } catch (Exception e) {
            log.error("检查系统负载失败", e);
            return SystemLoad.error();
        }
    }

    /**
     * 系统负载信息类
     */
    public static class SystemLoad {
        private final double memoryUsagePercent;
        private final double diskUsagePercent;
        private final long freeDiskSpaceGb;
        private final boolean databaseHealthy;
        private final int cpuCores;
        private final boolean healthy;

        public SystemLoad(double memoryUsagePercent, double diskUsagePercent,
                          long freeDiskSpaceGb, boolean databaseHealthy, int cpuCores) {
            this.memoryUsagePercent = memoryUsagePercent;
            this.diskUsagePercent = diskUsagePercent;
            this.freeDiskSpaceGb = freeDiskSpaceGb;
            this.databaseHealthy = databaseHealthy;
            this.cpuCores = cpuCores;
            this.healthy = memoryUsagePercent < 85 && diskUsagePercent < 90 &&
                          freeDiskSpaceGb > 5 && databaseHealthy;
        }

        public static SystemLoad error() {
            return new SystemLoad(0, 0, 0, false, 0);
        }

        // Getters
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public double getDiskUsagePercent() { return diskUsagePercent; }
        public long getFreeDiskSpaceGb() { return freeDiskSpaceGb; }
        public boolean isDatabaseHealthy() { return databaseHealthy; }
        public int getCpuCores() { return cpuCores; }
        public boolean isHealthy() { return healthy; }
    }
}