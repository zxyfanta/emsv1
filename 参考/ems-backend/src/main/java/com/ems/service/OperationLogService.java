package com.ems.service;

import com.ems.exception.BusinessException;
import com.ems.exception.ValidationException;
import com.ems.entity.OperationLog;
import com.ems.entity.User;
import com.ems.exception.ErrorCode;
import com.ems.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务层
 * 提供操作日志的记录和查询功能
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 记录操作日志
     *
     * @param user          用户
     * @param operationType 操作类型
     * @param resourceType  资源类型
     * @param resourceId    资源ID
     * @param resourceName  资源名称
     * @param description   操作描述
     * @param request       HTTP请求
     * @return 操作日志
     */
    @Async
    @Transactional
    public OperationLog logOperation(User user,
                                   OperationLog.OperationType operationType,
                                   OperationLog.ResourceType resourceType,
                                   String resourceId,
                                   String resourceName,
                                   String description,
                                   HttpServletRequest request) {
        return logOperation(user, operationType, resourceType, resourceId, resourceName,
                description, true, null, null, request, null, null);
    }

    /**
     * 记录操作日志（详细版）
     *
     * @param user          用户
     * @param operationType 操作类型
     * @param resourceType  资源类型
     * @param resourceId    资源ID
     * @param resourceName  资源名称
     * @param description   操作描述
     * @param success       操作是否成功
     * @param errorMessage  错误信息
     * @param duration      操作时长
     * @param request       HTTP请求
     * @param oldData       操作前数据
     * @param newData       操作后数据
     * @return 操作日志
     */
    @Async
    @Transactional
    public OperationLog logOperation(User user,
                                   OperationLog.OperationType operationType,
                                   OperationLog.ResourceType resourceType,
                                   String resourceId,
                                   String resourceName,
                                   String description,
                                   Boolean success,
                                   String errorMessage,
                                   Long duration,
                                   HttpServletRequest request,
                                   String oldData,
                                   String newData) {
        try {
            OperationLog operationLog = OperationLog.builder()
                    .user(user)
                    .username(user != null ? user.getUsername() : null)
                    .operationType(operationType)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .description(description)
                    .success(success)
                    .errorMessage(errorMessage)
                    .duration(duration)
                    .clientIp(getClientIp(request))
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .oldData(oldData)
                    .newData(newData)
                    .requestMethod(request != null ? request.getMethod() : null)
                    .requestUrl(request != null ? request.getRequestURL().toString() : null)
                    .requestParams(extractRequestParams(request))
                    .isSensitive(isSensitiveOperation(operationType, resourceType))
                    .verificationStatus(OperationLog.VerificationStatus.NONE)
                    .build();

            return operationLogRepository.save(operationLog);

        } catch (Exception e) {
            log.error("记录操作日志失败", e);
            // 不抛出异常，避免影响主要业务
            return null;
        }
    }

    /**
     * 记录登录日志
     *
     * @param user     用户
     * @param success  是否成功
     * @param clientIp 客户端IP
     * @return 操作日志
     */
    @Async
    @Transactional
    public OperationLog logLogin(User user, Boolean success, String clientIp, String userAgent) {
        try {
            String description = success ? "登录成功" : "登录失败";
            String errorMessage = success ? null : "用户名或密码错误";

            OperationLog operationLog = OperationLog.builder()
                    .user(user)
                    .username(user != null ? user.getUsername() : null)
                    .operationType(OperationLog.OperationType.LOGIN)
                    .resourceType(OperationLog.ResourceType.AUTHENTICATION)
                    .description(description)
                    .success(success)
                    .errorMessage(errorMessage)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .isSensitive(true)
                    .verificationStatus(OperationLog.VerificationStatus.NONE)
                    .build();

            return operationLogRepository.save(operationLog);

        } catch (Exception e) {
            log.error("记录登录日志失败", e);
            return null;
        }
    }

    /**
     * 分页查询操作日志
     *
     * @param pageable       分页参数
     * @param userId         用户ID（可选）
     * @param username       用户名（可选，模糊查询）
     * @param operationType  操作类型（可选）
     * @param resourceType   资源类型（可选）
     * @param success        操作结果（可选）
     * @param isSensitive    是否敏感操作（可选）
     * @param clientIp       客户端IP（可选）
     * @param startTime      开始时间（可选）
     * @param endTime        结束时间（可选）
     * @return 操作日志分页
     */
    public Page<OperationLog> getOperationLogs(Pageable pageable,
                                              Long userId,
                                              String username,
                                              OperationLog.OperationType operationType,
                                              OperationLog.ResourceType resourceType,
                                              Boolean success,
                                              Boolean isSensitive,
                                              String clientIp,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime) {
        Specification<OperationLog> specification = buildSearchSpecification(
                userId, username, operationType, resourceType, success, isSensitive, clientIp, startTime, endTime);

        return operationLogRepository.findAll(specification, pageable);
    }

    /**
     * 根据用户查询操作日志
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    public Page<OperationLog> getOperationLogsByUser(Long userId, Pageable pageable) {
        return operationLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 根据时间范围查询操作日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 操作日志分页
     */
    public Page<OperationLog> getOperationLogsByTimeRange(LocalDateTime startTime,
                                                        LocalDateTime endTime,
                                                        Pageable pageable) {
        return operationLogRepository.findByCreatedAtBetween(startTime, endTime, pageable);
    }

    /**
     * 查询失败的操作日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    public Page<OperationLog> getFailedOperationLogs(Pageable pageable) {
        return operationLogRepository.findBySuccessFalseOrderByCreatedAtDesc(pageable);
    }

    /**
     * 查询敏感操作日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    public Page<OperationLog> getSensitiveOperationLogs(Pageable pageable) {
        return operationLogRepository.findByIsSensitiveTrueOrderByCreatedAtDesc(pageable);
    }

    /**
     * 查询待二次验证的日志
     *
     * @param pageable 分页参数
     * @return 操作日志分页
     */
    public Page<OperationLog> getPendingVerificationLogs(Pageable pageable) {
        return operationLogRepository.findPendingVerificationLogs(pageable);
    }

    /**
     * 获取操作统计信息
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 统计信息
     */
    public Map<String, Object> getOperationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        // 获取基础统计
        List<Object[]> typeResults = operationLogRepository.countByOperationType();
        List<Object[]> resourceResults = operationLogRepository.countByResourceType();
        List<Object[]> successResults = operationLogRepository.countBySuccess();

        // 获取用户操作统计（最近30天）
        List<Object[]> userResults = operationLogRepository.countByUserLast30Days(10);

        // 获取时间范围内的操作总数
        Long totalCount = null;
        if (startTime != null && endTime != null) {
            totalCount = operationLogRepository.countLogsInTimeRange(startTime, endTime);
        }

        Map<String, Object> statistics = Map.of(
                "byOperationType", typeResults.stream()
                        .collect(Collectors.toMap(
                                result -> ((OperationLog.OperationType) result[0]).getDescription(),
                                result -> result[1]
                        )),
                "byResourceType", resourceResults.stream()
                        .collect(Collectors.toMap(
                                result -> ((OperationLog.ResourceType) result[0]).getDescription(),
                                result -> result[1]
                        )),
                "bySuccess", successResults.stream()
                        .collect(Collectors.toMap(
                                result -> (Boolean) result[0] ? "成功" : "失败",
                                result -> result[1]
                        )),
                "topUsers", userResults.stream()
                        .collect(Collectors.toMap(
                                result -> (String) result[0],
                                result -> result[1]
                        )),
                "totalCount", totalCount
        );

        return statistics;
    }

    /**
     * 获取异常IP列表
     *
     * @param hours     时间范围（小时）
     * @param threshold 失败次数阈值
     * @return 异常IP列表
     */
    public List<Map<String, Object>> getSuspiciousIps(int hours, int threshold) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);

        List<Object[]> results = operationLogRepository.findSuspiciousIps(startTime, endTime, threshold);

        return results.stream()
                .map(result -> Map.of(
                        "ip", result[0],
                        "failCount", result[1]
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取不活跃用户列表
     *
     * @param days 天数
     * @return 不活跃用户列表
     */
    public List<String> getInactiveUsers(int days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        return operationLogRepository.findInactiveUsers(beforeTime);
    }

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间阈值
     * @return 删除数量
     */
    @Transactional
    public int deleteLogsBefore(LocalDateTime beforeTime) {
        int deletedCount = operationLogRepository.deleteLogsBefore(beforeTime);
        log.info("删除操作日志完成: beforeTime={}, deletedCount={}", beforeTime, deletedCount);
        return deletedCount;
    }

    /**
     * 清理过期日志
     *
     * @param retentionDays 保留天数
     * @return 清理数量
     */
    @Transactional
    public int cleanupExpiredLogs(int retentionDays) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(retentionDays);
        return deleteLogsBefore(beforeTime);
    }

    /**
     * 更新操作日志的二次验证状态
     *
     * @param logId           日志ID
     * @param verificationStatus 验证状态
     */
    @Transactional
    public void updateVerificationStatus(Long logId, OperationLog.VerificationStatus verificationStatus) {
        OperationLog operationLog = operationLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "操作日志不存在"));

        operationLog.setVerificationStatus(verificationStatus);
        operationLogRepository.save(operationLog);

        log.info("更新操作日志验证状态成功: logId={}, status={}", logId, verificationStatus);
    }

    /**
     * 构建搜索条件
     */
    private Specification<OperationLog> buildSearchSpecification(Long userId,
                                                               String username,
                                                               OperationLog.OperationType operationType,
                                                               OperationLog.ResourceType resourceType,
                                                               Boolean success,
                                                               Boolean isSensitive,
                                                               String clientIp,
                                                               LocalDateTime startTime,
                                                               LocalDateTime endTime) {
        return (root, query, criteriaBuilder) -> {
            jakarta.persistence.criteria.Predicate predicate = criteriaBuilder.conjunction();

            // 用户ID条件
            if (userId != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            // 用户名条件
            if (StringUtils.hasText(username)) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.like(root.get("username"), "%" + username.toLowerCase() + "%"));
            }

            // 操作类型条件
            if (operationType != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("operationType"), operationType));
            }

            // 资源类型条件
            if (resourceType != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("resourceType"), resourceType));
            }

            // 操作结果条件
            if (success != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("success"), success));
            }

            // 敏感操作条件
            if (isSensitive != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("isSensitive"), isSensitive));
            }

            // 客户端IP条件
            if (StringUtils.hasText(clientIp)) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("clientIp"), clientIp));
            }

            // 时间范围条件
            if (startTime != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }

            if (endTime != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            return predicate;
        };
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 提取请求参数
     */
    private String extractRequestParams(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            // 这里简化实现，实际应该根据需要提取敏感参数并进行脱敏
            StringBuilder params = new StringBuilder();
            request.getParameterMap().forEach((key, values) -> {
                if (params.length() > 0) {
                    params.append("&");
                }
                params.append(key).append("=");
                // 敏感参数脱敏处理
                if (isSensitiveParameter(key)) {
                    params.append("***");
                } else {
                    params.append(String.join(",", values));
                }
            });
            return params.toString();
        } catch (Exception e) {
            log.warn("提取请求参数失败", e);
            return null;
        }
    }

    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParameter(String paramName) {
        String lowerParam = paramName.toLowerCase();
        return lowerParam.contains("password") ||
               lowerParam.contains("token") ||
               lowerParam.contains("secret") ||
               lowerParam.contains("key");
    }

    /**
     * 判断是否为敏感操作
     */
    private boolean isSensitiveOperation(OperationLog.OperationType operationType,
                                        OperationLog.ResourceType resourceType) {
        // 删除操作都是敏感的
        if (OperationLog.OperationType.DELETE.equals(operationType)) {
            return true;
        }

        // 用户和权限相关的操作是敏感的
        if (OperationLog.ResourceType.USER.equals(resourceType) ||
            OperationLog.ResourceType.PERMISSION.equals(resourceType) ||
            OperationLog.ResourceType.ROLE.equals(resourceType)) {
            return true;
        }

        // 系统配置操作是敏感的
        if (OperationLog.ResourceType.CONFIG.equals(resourceType) ||
            OperationLog.ResourceType.SYSTEM.equals(resourceType)) {
            return true;
        }

        return false;
    }
}