package com.ems.repository;

import com.ems.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层
 *
 * @author EMS Team
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    /**
     * 根据用户查找操作日志
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据操作类型查找日志
     *
     * @param operationType 操作类型
     * @param pageable       分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByOperationType(OperationLog.OperationType operationType, Pageable pageable);

    /**
     * 根据资源类型查找日志
     *
     * @param resourceType 资源类型
     * @param pageable     分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByResourceType(OperationLog.ResourceType resourceType, Pageable pageable);

    /**
     * 根据用户名查找日志
     *
     * @param username 用户名
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /**
     * 根据操作结果查找日志
     *
     * @param success  是否成功
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findBySuccess(Boolean success, Pageable pageable);

    /**
     * 根据敏感操作查找日志
     *
     * @param isSensitive 是否敏感操作
     * @param pageable    分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByIsSensitive(Boolean isSensitive, Pageable pageable);

    /**
     * 根据客户端IP查找日志
     *
     * @param clientIp  客户端IP
     * @param pageable  分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByClientIp(String clientIp, Pageable pageable);

    /**
     * 根据时间范围查找日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 操作日志分页
     */
    @Query("SELECT ol FROM OperationLog ol WHERE ol.createdAt BETWEEN :startTime AND :endTime")
    Page<OperationLog> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             Pageable pageable);

    /**
     * 根据用户和时间范围查找日志
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 操作日志分页
     */
    @Query("SELECT ol FROM OperationLog ol WHERE ol.user.id = :userId AND ol.createdAt BETWEEN :startTime AND :endTime")
    Page<OperationLog> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       Pageable pageable);

    /**
     * 根据操作类型和时间范围查找日志
     *
     * @param operationType 操作类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param pageable      分页参数
     * @return 操作日志分页
     */
    @Query("SELECT ol FROM OperationLog ol WHERE ol.operationType = :operationType AND ol.createdAt BETWEEN :startTime AND :endTime")
    Page<OperationLog> findByOperationTypeAndCreatedAtBetween(@Param("operationType") OperationLog.OperationType operationType,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime,
                                                             Pageable pageable);

    /**
     * 查找失败的操作日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findBySuccessFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找敏感操作日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByIsSensitiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找需要二次验证的日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    @Query("SELECT ol FROM OperationLog ol WHERE ol.isSensitive = true AND (ol.verificationStatus IS NULL OR ol.verificationStatus = 'PENDING')")
    Page<OperationLog> findPendingVerificationLogs(Pageable pageable);

    /**
     * 统计各操作类型的数量
     *
     * @return 统计结果
     */
    @Query("SELECT ol.operationType, COUNT(ol) FROM OperationLog ol GROUP BY ol.operationType")
    List<Object[]> countByOperationType();

    /**
     * 统计各资源类型的数量
     *
     * @return 统计结果
     */
    @Query("SELECT ol.resourceType, COUNT(ol) FROM OperationLog ol GROUP BY ol.resourceType")
    List<Object[]> countByResourceType();

    /**
     * 统计成功和失败的操作数量
     *
     * @return 统计结果
     */
    @Query("SELECT ol.success, COUNT(ol) FROM OperationLog ol GROUP BY ol.success")
    List<Object[]> countBySuccess();

    /**
     * 统计用户的操作数量
     *
     * @param limit 限制数量
     * @return 统计结果
     */
    @Query(value = "SELECT ol.username, COUNT(*) as count FROM operation_logs ol " +
           "WHERE ol.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
           "GROUP BY ol.username ORDER BY count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> countByUserLast30Days(@Param("limit") int limit);

    /**
     * 查找最近的登录日志
     *
     * @param username 用户名
     * @param limit    限制数量
     * @return 操作日志列表
     */
    @Query("SELECT ol FROM OperationLog ol WHERE ol.operationType = 'LOGIN' AND ol.username = :username " +
           "ORDER BY ol.createdAt DESC")
    List<OperationLog> findRecentLoginLogs(@Param("username") String username, Pageable pageable);

    /**
     * 查找异常IP的操作
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param threshold 失败次数阈值
     * @return IP地址列表
     */
    @Query(value = "SELECT client_ip, COUNT(*) as fail_count FROM operation_logs " +
           "WHERE success = false AND created_at BETWEEN :startTime AND :endTime " +
           "GROUP BY client_ip HAVING fail_count >= :threshold", nativeQuery = true)
    List<Object[]> findSuspiciousIps(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("threshold") int threshold);

    /**
     * 查找长时间未操作的用户
     *
     * @param beforeTime 时间阈值
     * @return 用户名列表
     */
    @Query(value = "SELECT DISTINCT username FROM operation_logs " +
           "WHERE created_at < :beforeTime " +
           "AND username NOT IN (SELECT DISTINCT username FROM operation_logs WHERE created_at >= :beforeTime)", nativeQuery = true)
    List<String> findInactiveUsers(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间阈值
     * @return 删除行数
     */
    @Modifying
    @Query("DELETE FROM OperationLog ol WHERE ol.createdAt < :beforeTime")
    int deleteLogsBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定时间范围内的操作数量
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 操作数量
     */
    @Query("SELECT COUNT(ol) FROM OperationLog ol WHERE ol.createdAt BETWEEN :startTime AND :endTime")
    Long countLogsInTimeRange(@Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 查找操作时长超过阈值的日志
     *
     * @param durationThreshold 时长阈值（毫秒）
     * @param pageable         分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByDurationGreaterThanOrderByDurationDesc(Long durationThreshold, Pageable pageable);

    /**
     * 根据资源ID查找日志
     *
     * @param resourceId 资源ID
     * @param pageable   分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByResourceIdOrderByCreatedAtDesc(String resourceId, Pageable pageable);

    /**
     * 查找包含错误信息的日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    Page<OperationLog> findByErrorMessageIsNotNullOrderByCreatedAtDesc(Pageable pageable);
}