package com.cdutetc.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据备份配置属性
 * 从application.yaml读取配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ems.backup")
public class BackupProperties {

    /**
     * 是否启用备份功能
     */
    private boolean enabled = true;

    /**
     * 备份根目录
     */
    private String rootDir = "/backup/ems";

    /**
     * 数据库配置
     */
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * 时序数据备份配置
     */
    private TimeseriesBackupConfig timeseries = new TimeseriesBackupConfig();

    /**
     * 业务数据备份配置
     */
    private BusinessBackupConfig business = new BusinessBackupConfig();

    /**
     * 系统数据备份配置
     */
    private SystemBackupConfig system = new SystemBackupConfig();

    /**
     * 定时任务配置
     */
    private SchedulerConfig scheduler = new SchedulerConfig();

    /**
     * 高级配置
     */
    private AdvancedConfig advanced = new AdvancedConfig();

    @Data
    public static class DatabaseConfig {
        private String name = "";
        private String host = "";
        private Integer port = null;
        private String username = "";
        private String password = "";
    }

    @Data
    public static class TimeseriesBackupConfig {
        private boolean enabled = true;
        private int retentionMonths = 6;
        private List<String> tables = List.of(
            "ems_radiation_device_data",
            "ems_environment_device_data"
        );
        private String subDir = "timeseries";
        private String filePrefix = "timeseries_data";
    }

    @Data
    public static class BusinessBackupConfig {
        private boolean enabled = true;
        private int retentionMonths = 6;
        private List<String> tables = List.of(
            "alerts",
            "ems_data_report_log"
        );
        private String subDir = "business";
        private String filePrefix = "business_data";
    }

    @Data
    public static class SystemBackupConfig {
        private boolean enabled = true;
        private int keepCount = 2;
        private List<String> tables = List.of(
            "ems_company",
            "ems_user",
            "ems_device",
            "ems_device_activation_code",
            "video_devices"
        );
        private String subDir = "system";
        private String filePrefix = "system_config";
    }

    @Data
    public static class SchedulerConfig {
        private String cron = "0 3 1 * *";
        private boolean enabled = true;
    }

    @Data
    public static class AdvancedConfig {
        private boolean compress = true;
        private String compressFormat = "gzip";
        private boolean verifyBeforeDelete = true;
        private boolean verboseLogging = true;
        private int mysqlConnectTimeout = 30;
        private String mysqldumpPath = "";
        private String mysqlPath = "";
    }
}
