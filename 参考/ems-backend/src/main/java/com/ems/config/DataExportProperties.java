package com.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 月度数据导出配置属性
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ems.data-export")
public class DataExportProperties {

    /**
     * 是否启用数据导出
     */
    private boolean enabled = true;

    /**
     * 定时执行表达式 (每月1日凌晨2点)
     */
    private String cron = "0 0 2 1 * ?";

    /**
     * 保留数据月份数
     */
    private int keepMonths = 6;

    /**
     * 导出文件存储路径
     */
    private String exportPath = "/data/ems/exports";

    /**
     * 测试触发开关
     */
    private boolean testTrigger = false;

    /**
     * 导出格式 (sql, csv, json)
     */
    private String exportFormat = "sql";

    /**
     * 导出的数据表名
     */
    private String tableName = "device_status_records";

    /**
     * mysqldump命令路径
     */
    private String mysqldumpPath = "/usr/bin/mysqldump";

    /**
     * 最大导出时长(秒)
     */
    private int maxExportDuration = 14400; // 4小时

    /**
     * 导出批次大小
     */
    private int batchSize = 50000;

    /**
     * 最小磁盘空间要求(GB)
     */
    private long minDiskSpaceGb = 10L;

    /**
     * 是否启用文件压缩
     */
    private boolean enableCompression = true;

    /**
     * 数据库连接超时时间
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * 获取最大导出时长作为Duration
     */
    public Duration getMaxExportDuration() {
        return Duration.ofSeconds(maxExportDuration);
    }
}