package com.ems.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 数据导出日志实体
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Entity
@Table(name = "data_export_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataExportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 导出月份
     */
    @Column(name = "export_month", nullable = false)
    private LocalDate exportMonth;

    /**
     * 目标月份开始时间
     */
    @Column(name = "target_month_start", nullable = false)
    private LocalDateTime targetMonthStart;

    /**
     * 目标月份结束时间
     */
    @Column(name = "target_month_end", nullable = false)
    private LocalDateTime targetMonthEnd;

    /**
     * 导出文件路径
     */
    @Column(name = "export_file_path", length = 500)
    private String exportFilePath;

    /**
     * 导出前记录数
     */
    @Column(name = "records_before_export")
    private Long recordsBeforeExport;

    /**
     * 导出后记录数
     */
    @Column(name = "records_after_export")
    private Long recordsAfterExport;

    /**
     * 导出文件大小(字节)
     */
    @Column(name = "export_file_size_bytes")
    private Long exportFileSizeBytes;

    /**
     * 导出状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "export_status", nullable = false)
    private ExportStatus exportStatus;

    /**
     * 导出耗时(秒)
     */
    @Column(name = "export_duration_seconds")
    private Integer exportDurationSeconds;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 导出月份的记录数
     */
    @Column(name = "exported_records_count")
    private Long exportedRecordsCount;

    /**
     * 是否为测试执行
     */
    @Builder.Default
    @Column(name = "is_test_execution")
    private Boolean isTestExecution = false;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    @LastModifiedDate
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 导出状态枚举
     */
    public enum ExportStatus {
        PENDING("待执行"),
        RUNNING("执行中"),
        SUCCESS("成功"),
        FAILED("失败"),
        PARTIAL("部分成功");

        private final String description;

        ExportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取导出文件大小(GB)
     */
    public Double getExportFileSizeGb() {
        if (exportFileSizeBytes == null) {
            return null;
        }
        return exportFileSizeBytes / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * 获取删除的记录数
     */
    public Long getDeletedRecordsCount() {
        if (recordsBeforeExport == null || recordsAfterExport == null) {
            return null;
        }
        return recordsBeforeExport - recordsAfterExport;
    }
}