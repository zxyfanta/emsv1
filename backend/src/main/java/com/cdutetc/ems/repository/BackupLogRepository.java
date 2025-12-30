package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.BackupLog;
import com.cdutetc.ems.entity.BackupLog.BackupType;
import com.cdutetc.ems.entity.BackupLog.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据备份日志Repository
 */
@Repository
public interface BackupLogRepository extends JpaRepository<BackupLog, Long> {

    /**
     * 查询最近的备份记录
     */
    List<BackupLog> findTop10ByOrderByCreatedAtDesc();

    /**
     * 根据备份类型查询
     */
    List<BackupLog> findByBackupTypeOrderByCreatedAtDesc(BackupType backupType);

    /**
     * 查询运行中的备份任务
     */
    List<BackupLog> findByStatusOrderByCreatedAtDesc(BackupStatus status);

    /**
     * 查询指定时间范围内的备份记录
     */
    List<BackupLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * 统计备份记录数
     */
    @Query("SELECT COUNT(b) FROM BackupLog b WHERE b.status = :status")
    long countByStatus(BackupStatus status);

    /**
     * 查询最近的备份任务（用于检查是否已有任务在运行）
     */
    @Query("SELECT b FROM BackupLog b WHERE b.status = 'RUNNING' AND b.backupType = :type")
    List<BackupLog> findRunningBackupsByType(BackupType type);
}
