package com.ems.aspect;

import com.ems.annotation.OperationLogAnnotation;
import com.ems.entity.OperationLog;
import com.ems.entity.User;
import com.ems.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 操作日志切面
 * 使用AOP自动记录操作日志
 *
 * @author EMS Team
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 定义切点：所有带有@OperationLogAnnotation注解的方法
     */
    @Pointcut("@annotation(com.ems.annotation.OperationLogAnnotation)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知，记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object aroundOperationLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        OperationLogAnnotation annotation = method.getAnnotation(OperationLogAnnotation.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户
        User currentUser = getCurrentUser();

        // 获取HTTP请求
        HttpServletRequest request = getCurrentRequest();

        // 提取请求参数
        Object[] args = joinPoint.getArgs();
        String requestParams = extractRequestParams(args, annotation.excludeParams());

        OperationLog.OperationType operationType = annotation.operationType();
        OperationLog.ResourceType resourceType = annotation.resourceType();
        String description = getOperationDescription(annotation, args);

        Object result = null;
        Exception exception = null;
        String oldValue = null;
        String newValue = null;

        try {
            // 如果需要记录操作前的数据，在这里提取
            if (annotation.recordOldData()) {
                oldValue = extractOldData(operationType, resourceType, args);
            }

            // 执行目标方法
            result = joinPoint.proceed();

            // 如果需要记录操作后的数据，在这里提取
            if (annotation.recordNewData()) {
                newValue = extractNewData(result);
            }

            return result;

        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 计算操作时长
            long duration = System.currentTimeMillis() - startTime;

            // 异步记录操作日志
            logOperationAsync(currentUser, operationType, resourceType, description,
                    exception, duration, request, requestParams, oldValue, newValue);
        }
    }

    /**
     * 异步记录操作日志
     */
    private void logOperationAsync(User user,
                                  OperationLog.OperationType operationType,
                                  OperationLog.ResourceType resourceType,
                                  String description,
                                  Exception exception,
                                  Long duration,
                                  HttpServletRequest request,
                                  String requestParams,
                                  String oldValue,
                                  String newValue) {
        try {
            boolean success = exception == null;
            String errorMessage = exception != null ? exception.getMessage() : null;

            String resourceId = extractResourceId(user, request);
            String resourceName = extractResourceName(user, request, description);

            operationLogService.logOperation(
                    user,
                    operationType,
                    resourceType,
                    resourceId,
                    resourceName,
                    description,
                    success,
                    errorMessage,
                    duration,
                    request,
                    oldValue,
                    newValue
            );

        } catch (Exception e) {
            log.error("异步记录操作日志失败", e);
        }
    }

    /**
     * 获取当前用户
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败", e);
        }
        return null;
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("获取当前请求失败", e);
        }
        return null;
    }

    /**
     * 提取请求参数
     */
    private String extractRequestParams(Object[] args, String[] excludeParams) {
        try {
            if (args == null || args.length == 0) {
                return null;
            }

            // 简化实现，实际应该根据excludeParams过滤敏感参数
            StringBuilder params = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (params.length() > 0) {
                    params.append("&");
                }

                String paramName = "arg" + i;
                boolean shouldExclude = Arrays.asList(excludeParams).contains(paramName);

                if (shouldExclude) {
                    params.append(paramName).append("=***");
                } else {
                    params.append(paramName).append("=").append(toString(args[i]));
                }
            }

            return params.toString();

        } catch (Exception e) {
            log.warn("提取请求参数失败", e);
            return null;
        }
    }

    /**
     * 获取操作描述
     */
    private String getOperationDescription(OperationLogAnnotation annotation, Object[] args) {
        String description = annotation.value();
        if (StringUtils.isEmpty(description) && args != null && args.length > 0) {
            // 如果没有提供描述，尝试根据方法名和参数生成
            description = generateDescriptionFromArgs(args);
        }
        return description;
    }

    /**
     * 根据参数生成描述
     */
    private String generateDescriptionFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "执行操作";
        }

        StringBuilder sb = new StringBuilder("执行操作，参数：");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("arg").append(i).append("=").append(toString(args[i]));
        }

        return sb.toString();
    }

    /**
     * 提取资源ID
     */
    private String extractResourceId(User user, HttpServletRequest request) {
        // 从请求中提取资源ID
        if (request != null) {
            String path = request.getRequestURI();
            // 尝试从路径中提取ID，例如 /api/devices/123 -> 123
            String[] pathParts = path.split("/");
            if (pathParts.length > 0) {
                String lastPart = pathParts[pathParts.length - 1];
                try {
                    Long.parseLong(lastPart); // 验证是否为数字ID
                    return lastPart;
                } catch (NumberFormatException e) {
                    // 不是数字ID，忽略
                }
            }
        }

        // 如果无法从路径提取，返回用户ID作为默认值
        return user != null ? user.getId().toString() : null;
    }

    /**
     * 提取资源名称
     */
    private String extractResourceName(User user, HttpServletRequest request, String description) {
        // 简化实现，优先使用描述，其次使用用户名
        if (StringUtils.hasText(description)) {
            return description;
        }

        return user != null ? user.getUsername() : "未知";
    }

    /**
     * 提取操作前数据
     */
    private String extractOldData(OperationLog.OperationType operationType,
                                 OperationLog.ResourceType resourceType,
                                 Object[] args) {
        // 这里应该根据具体的业务逻辑来提取操作前的数据
        // 例如，更新操作需要先查询数据库获取旧数据
        try {
            if (OperationLog.OperationType.UPDATE.equals(operationType) && args.length > 0) {
                // 对于更新操作，第一个参数通常是ID
                String id = toString(args[0]);
                return "{\"id\": \"" + id + "\", \"status\": \"旧数据\"}";
            }
        } catch (Exception e) {
            log.debug("提取操作前数据失败", e);
        }

        return null;
    }

    /**
     * 提取操作后数据
     */
    private String extractNewData(Object result) {
        try {
            if (result != null) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            log.debug("提取操作后数据失败", e);
        }

        return null;
    }

    /**
     * 对象转字符串
     */
    private String toString(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return (String) obj;
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * 字符串工具方法
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean hasText(String str) {
        return !isEmpty(str);
    }
}