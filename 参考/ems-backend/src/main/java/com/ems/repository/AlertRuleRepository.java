package com.ems.repository;

import com.ems.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 告警规则数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * 根据设备ID查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.id = :deviceId AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 根据设备唯一编号查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.deviceId = :deviceId AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByDeviceDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID和启用状态查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.deviceId = :deviceId AND ar.enabled = :enabled AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByDeviceIdAndEnabled(@Param("deviceId") String deviceId, @Param("enabled") Boolean enabled);

    /**
     * 根据企业ID查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.enterprise.id = :enterpriseId AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 根据企业ID和启用状态查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.enterprise.id = :enterpriseId AND ar.enabled = :enabled AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByEnterpriseIdAndEnabled(@Param("enterpriseId") Long enterpriseId, @Param("enabled") Boolean enabled);

    /**
     * 根据指标类型查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.metricName = :metricName AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByMetricName(@Param("metricName") AlertRule.MetricName metricName);

    /**
     * 根据严重级别查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.severity = :severity AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findBySeverity(@Param("severity") AlertRule.AlertSeverity severity);

    /**
     * 查找启用的告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findEnabledRules();

    /**
     * 根据设备ID和指标类型查找告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.deviceId = :deviceId AND ar.metricName = :metricName AND ar.enabled = true AND ar.deleted = false")
    List<AlertRule> findByDeviceIdAndMetricNameAndEnabled(@Param("deviceId") String deviceId,
                                                         @Param("metricName") AlertRule.MetricName metricName);

    /**
     * 统计设备告警规则数量
     */
    @Query("SELECT COUNT(ar) FROM AlertRule ar WHERE ar.device.id = :deviceId AND ar.deleted = false")
    long countByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 统计企业告警规则数量
     */
    @Query("SELECT COUNT(ar) FROM AlertRule ar WHERE ar.device.enterprise.id = :enterpriseId AND ar.deleted = false")
    long countByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 检查设备是否存在指定指标的告警规则
     */
    @Query("SELECT COUNT(ar) > 0 FROM AlertRule ar WHERE ar.device.deviceId = :deviceId AND ar.metricName = :metricName AND ar.enabled = true AND ar.deleted = false")
    boolean existsByDeviceIdAndMetricNameAndEnabled(@Param("deviceId") String deviceId,
                                                   @Param("metricName") AlertRule.MetricName metricName);

    /**
     * 查找最近创建的告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findRecentRules(Pageable pageable);

    /**
     * 根据规则名称模糊查询
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.ruleName LIKE %:ruleName% AND ar.deleted = false ORDER BY ar.createdAt DESC")
    List<AlertRule> findByRuleNameContaining(@Param("ruleName") String ruleName);

    /**
     * 分页查询告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.deleted = false ORDER BY ar.createdAt DESC")
    Page<AlertRule> findAllByDeletedFalse(Pageable pageable);

    /**
     * 根据设备ID分页查询告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.id = :deviceId AND ar.deleted = false ORDER BY ar.createdAt DESC")
    Page<AlertRule> findByDeviceIdAndDeletedFalse(@Param("deviceId") Long deviceId, Pageable pageable);

    /**
     * 根据企业ID分页查询告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.device.enterprise.id = :enterpriseId AND ar.deleted = false ORDER BY ar.createdAt DESC")
    Page<AlertRule> findByEnterpriseIdAndDeletedFalse(@Param("enterpriseId") Long enterpriseId, Pageable pageable);

    /**
     * 根据ID查找未删除的告警规则
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.id = :id AND ar.deleted = false")
    Optional<AlertRule> findByIdAndDeletedFalse(@Param("id") Long id);
}