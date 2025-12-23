package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警Repository
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 按企业查找告警（分页）
     */
    Page<Alert> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * 按企业和解决状态查找告警
     */
    Page<Alert> findByCompanyIdAndResolved(Long companyId, Boolean resolved, Pageable pageable);

    /**
     * 按设备查找告警
     */
    @Query("SELECT a FROM Alert a WHERE a.device.id = :deviceId ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 按设备编码查找告警
     */
    @Query("SELECT a FROM Alert a WHERE a.deviceCode = :deviceCode ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 按告警类型查找
     */
    @Query("SELECT a FROM Alert a WHERE a.company.id = :companyId AND a.alertType = :alertType ORDER BY a.createdAt DESC")
    List<Alert> findByCompanyIdAndAlertType(@Param("companyId") Long companyId, @Param("alertType") String alertType);

    /**
     * 查找未解决的告警
     */
    @Query("SELECT a FROM Alert a WHERE a.company.id = :companyId AND a.resolved = false ORDER BY a.createdAt DESC")
    List<Alert> findUnresolvedAlerts(@Param("companyId") Long companyId);

    /**
     * 查找指定时间范围内的告警
     */
    @Query("SELECT a FROM Alert a WHERE a.company.id = :companyId AND a.createdAt BETWEEN :startTime AND :endTime ORDER BY a.createdAt DESC")
    List<Alert> findByCompanyIdAndTimeRange(@Param("companyId") Long companyId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计未解决的告警数量
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.company.id = :companyId AND a.resolved = false")
    long countUnresolvedAlerts(@Param("companyId") Long companyId);

    /**
     * 统计按严重程度分组的告警数量
     */
    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.company.id = :companyId AND a.resolved = false GROUP BY a.severity")
    List<Object[]> countAlertsBySeverityGrouped(@Param("companyId") Long companyId);

    /**
     * 查找最近的N条告警
     */
    @Query("SELECT a FROM Alert a WHERE a.company.id = :companyId ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("companyId") Long companyId, Pageable pageable);
}
