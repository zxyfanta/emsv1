package com.cdutetc.ems.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 数据备份日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ems_backup_log")
public class BackupLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 备份类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", nullable = false, length = 20)
    private BackupType backupType;

    /**
     * 备份状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BackupStatus status;

    /**
     * 备份文件路径
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * 备份文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 备份记录数
     */
    @Column(name = "record_count")
    private Integer recordCount;

    /**
     * 删除记录数
     */
    @Column(name = "deleted_count")
    private Integer deletedCount;

    /**
     * 保留月数
     */
    @Column(name = "retention_months")
    private Integer retentionMonths;

    /**
     * 截止日期
     */
    @Column(name = "cutoff_date", length = 50)
    private String cutoffDate;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 触发方式（MANUAL/SCHEDULED）
     */
    @Column(name = "trigger_mode", length = 20)
    private String triggerMode;

    /**
     * 备份的表列表（JSON格式）
     */
    @Column(name = "tables", columnDefinition = "TEXT")
    private String tables;

    /**
     * 备份类型枚举
     */
    public enum BackupType {
        TIMESERIES,   // 时序数据
        BUSINESS,     // 业务数据
        SYSTEM,       // 系统配置数据
        FULL          // 全量备份
    }

    /**
     * 备份状态枚举
     */
    public enum BackupStatus {
        RUNNING,      // 运行中
        SUCCESS,      // 成功
        FAILED,       // 失败
        PARTIAL       // 部分成功
    }
}
