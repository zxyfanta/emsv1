package com.ems.repository;

import com.ems.entity.DataExportLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 数据导出日志Repository
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Repository
public interface DataExportLogRepository extends JpaRepository<DataExportLog, Long>,
        JpaSpecificationExecutor<DataExportLog> {

    /**
     * 根据导出月份查找日志
     */
    Optional<DataExportLog> findByExportMonth(LocalDate exportMonth);

    /**
     * 根据导出状态查找日志
     */
    List<DataExportLog> findByExportStatus(DataExportLog.ExportStatus status);

    /**
     * 根据是否为测试执行查找日志
     */
    List<DataExportLog> findByIsTestExecutionOrderByCreatedAtDesc(Boolean isTestExecution);

    /**
     * 查找指定时间范围内的导出日志
     */
    List<DataExportLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找最近的成功导出日志
     */
    @Query("SELECT d FROM DataExportLog d WHERE d.exportStatus = 'SUCCESS' AND d.isTestExecution = false ORDER BY d.createdAt DESC")
    List<DataExportLog> findRecentSuccessfulExports(Pageable pageable);

    /**
     * 统计指定状态的数量
     */
    @Query("SELECT COUNT(d) FROM DataExportLog d WHERE d.exportStatus = :status")
    long countByExportStatus(@Param("status") DataExportLog.ExportStatus status);

    /**
     * 获取导出统计信息
     */
    @Query("SELECT d.exportMonth, d.exportStatus, d.exportedRecordsCount, d.exportFileSizeBytes, d.exportDurationSeconds " +
           "FROM DataExportLog d WHERE d.isTestExecution = false ORDER BY d.exportMonth DESC")
    List<Object[]> getExportStatistics();

    /**
     * 查找指定月份数据量的导出历史
     */
    @Query("SELECT d FROM DataExportLog d WHERE d.exportedRecordsCount IS NOT NULL ORDER BY d.exportedRecordsCount DESC")
    List<DataExportLog> findByExportedRecordsCountIsNotNullOrderByExportedRecordsCountDesc();

    /**
     * 查找失败的导出记录
     */
    @Query("SELECT d FROM DataExportLog d WHERE d.exportStatus IN ('FAILED', 'PARTIAL') ORDER BY d.createdAt DESC")
    List<DataExportLog> findFailedExports(Pageable pageable);

    /**
     * 获取最近N个月的导出记录
     */
    @Query("SELECT d FROM DataExportLog d WHERE d.exportMonth >= :startDate ORDER BY d.exportMonth DESC")
    List<DataExportLog> findRecentMonthsExports(@Param("startDate") LocalDate startDate);

    /**
     * 检查是否有正在运行的任务
     */
    @Query("SELECT COUNT(d) FROM DataExportLog d WHERE d.exportStatus = 'RUNNING'")
    long countRunningExports();

    /**
     * 查找长时间运行的导出任务
     */
    @Query("SELECT d FROM DataExportLog d WHERE d.exportStatus = 'RUNNING' AND d.createdAt < :threshold")
    List<DataExportLog> findByExportStatusAndCreatedAtBefore(@Param("exportStatus") DataExportLog.ExportStatus exportStatus,
                                                            @Param("threshold") LocalDateTime threshold);
}