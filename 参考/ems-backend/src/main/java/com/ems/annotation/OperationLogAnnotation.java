package com.ems.annotation;

import com.ems.entity.OperationLog;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 *
 * @author EMS Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogAnnotation {

    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 操作类型
     */
    @AliasFor("operationType")
    OperationLog.OperationType type() default OperationLog.OperationType.READ;

    /**
     * 操作类型（别名）
     */
    @AliasFor("type")
    OperationLog.OperationType operationType() default OperationLog.OperationType.READ;

    /**
     * 资源类型
     */
    @AliasFor("resourceType")
    OperationLog.ResourceType module() default OperationLog.ResourceType.SYSTEM;

    /**
     * 资源类型（别名）
     */
    @AliasFor("module")
    OperationLog.ResourceType resourceType() default OperationLog.ResourceType.SYSTEM;

    /**
     * 是否记录操作前的数据
     */
    boolean recordOldData() default false;

    /**
     * 是否记录操作后的数据
     */
    boolean recordNewData() default false;

    /**
     * 需要排除的请求参数
     */
    String[] excludeParams() default {};

    /**
     * 是否为敏感操作
     */
    boolean sensitive() default false;

    /**
     * 操作说明（更详细的描述）
     */
    String description() default "";
}