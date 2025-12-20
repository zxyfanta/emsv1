package com.ems.repository;

import com.ems.entity.AlertRecord;
import com.ems.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警记录数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    /**
     * 根据设备ID查找告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID分页查找告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId ORDER BY ar.triggeredAt DESC")
    Page<AlertRecord> findByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 根据企业ID查找告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.enterprise.id = :enterpriseId ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 根据企业ID分页查找告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.enterprise.id = :enterpriseId ORDER BY ar.triggeredAt DESC")
    Page<AlertRecord> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId, Pageable pageable);

    /**
     * 根据告警状态查找记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.status = :status ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByStatus(@Param("status") AlertRecord.AlertStatus status);

    /**
     * 根据严重级别查找记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.severity = :severity ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findBySeverity(@Param("severity") String severity);

    /**
     * 根据设备ID和状态查找记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId AND ar.status = :status ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByDeviceIdAndStatus(@Param("deviceId") String deviceId, @Param("status") AlertRecord.AlertStatus status);

    /**
     * 查找指定时间范围内的告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.triggeredAt BETWEEN :startTime AND :endTime ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByTriggeredAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定设备在时间范围内的告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId AND ar.triggeredAt BETWEEN :startTime AND :endTime ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByDeviceIdAndTriggeredAtBetween(@Param("deviceId") String deviceId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查找活跃的告警记录（未解决的）
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.status IN ('ACTIVE', 'ACKNOWLEDGED') ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findActiveAlerts();

    /**
     * 查找指定设备的活跃告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId AND ar.status IN ('ACTIVE', 'ACKNOWLEDGED') ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findActiveAlertsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 查找未解决的告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.status = 'ACTIVE' ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findUnresolvedAlerts();

    /**
     * 统计企业告警记录数量
     */
    @Query("SELECT COUNT(ar) FROM AlertRecord ar WHERE ar.device.enterprise.id = :enterpriseId")
    long countByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 统计设备告警记录数量
     */
    @Query("SELECT COUNT(ar) FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId")
    long countByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 统计告警状态数量
     */
    @Query("SELECT ar.status, COUNT(ar) FROM AlertRecord ar GROUP BY ar.status")
    List<Object[]> countAlertsByStatus();

    /**
     * 统计严重级别数量
     */
    @Query("SELECT ar.severity, COUNT(ar) FROM AlertRecord ar GROUP BY ar.severity")
    List<Object[]> countAlertsBySeverity();

    /**
     * 查找最近的告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findRecentAlerts(Pageable pageable);

    /**
     * 查找已解决的告警记录（在指定时间范围内）
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.status = 'RESOLVED' AND ar.resolvedAt BETWEEN :startTime AND :endTime ORDER BY ar.resolvedAt DESC")
    List<AlertRecord> findResolvedAlertsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据告警规则ID查找记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.rule.id = :ruleId ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 查找需要发送通知的告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.notificationSent = false AND ar.status = 'ACTIVE' ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findAlertsNeedingNotification();

    /**
     * 查找超过指定时间未解决的告警
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.status = 'ACTIVE' AND ar.triggeredAt < :threshold ORDER BY ar.triggeredAt ASC")
    List<AlertRecord> findOverdueAlerts(@Param("threshold") LocalDateTime threshold);

    /**
     * 查找设备最新的告警记录
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId ORDER BY ar.triggeredAt DESC")
    List<AlertRecord> findLatestByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 查找设备未解决的告警数量
     */
    @Query("SELECT COUNT(ar) FROM AlertRecord ar WHERE ar.device.deviceId = :deviceId AND ar.status IN ('ACTIVE', 'ACKNOWLEDGED')")
    long countActiveAlertsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 删除指定时间之前的告警记录
     */
    @Modifying
    @Query("DELETE FROM AlertRecord ar WHERE ar.triggeredAt < :thresholdDate")
    int deleteByTriggeredAtBefore(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * 统计指定时间范围内各严重级别的告警数量
     */
    @Query("SELECT ar.severity, COUNT(ar) FROM AlertRecord ar WHERE ar.triggeredAt BETWEEN :startTime AND :endTime GROUP BY ar.severity")
    List<Object[]> countAlertsBySeverityBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询告警记录（支持多条件过滤）
     */
    @Query("SELECT ar FROM AlertRecord ar WHERE " +
           "(:deviceId IS NULL OR ar.device.deviceId = :deviceId) AND " +
           "(:severity IS NULL OR ar.severity = :severity) AND " +
           "(:status IS NULL OR ar.status = :status) AND " +
           "(:startTime IS NULL OR ar.triggeredAt >= :startTime) AND " +
           "(:endTime IS NULL OR ar.triggeredAt <= :endTime) " +
           "ORDER BY ar.triggeredAt DESC")
    Page<AlertRecord> findAlertsWithFilters(@Param("deviceId") String deviceId,
                                           @Param("severity") AlertRule.AlertSeverity severity,
                                           @Param("status") AlertRecord.AlertStatus status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           Pageable pageable);
}