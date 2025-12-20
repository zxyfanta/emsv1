package com.ems.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 用于记录用户的操作行为
 *
 * @author EMS Team
 */
@Entity
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_log_user_id", columnList = "user_id"),
    @Index(name = "idx_log_operation_type", columnList = "operation_type"),
    @Index(name = "idx_log_resource_type", columnList = "resource_type"),
    @Index(name = "idx_log_created_at", columnList = "created_at"),
    @Index(name = "idx_log_success", columnList = "success"),
    @Index(name = "idx_log_user_time", columnList = "user_id,created_at"),
    @Index(name = "idx_log_operation_time", columnList = "operation_type,created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作用户
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 用户名（冗余字段，便于查询）
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationType operationType;

    /**
     * 资源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private ResourceType resourceType;

    /**
     * 资源ID
     */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /**
     * 资源名称
     */
    @Column(name = "resource_name", length = 200)
    private String resourceName;

    /**
     * 操作描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 请求方法
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * 请求URL
     */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /**
     * 请求参数（JSON格式）
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 操作结果（成功/失败）
     */
    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 操作时长（毫秒）
     */
    @Column(name = "duration")
    private Long duration;

    /**
     * 客户端IP地址
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 操作位置（地理位置）
     */
    @Column(name = "location", length = 200)
    private String location;

    /**
     * 是否敏感操作
     */
    @Column(name = "is_sensitive", nullable = false)
    @Builder.Default
    private Boolean isSensitive = false;

    /**
     * 二次验证状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    private VerificationStatus verificationStatus;

    /**
     * 操作前的数据（JSON格式）
     */
    @Column(name = "old_data", columnDefinition = "TEXT")
    private String oldData;

    /**
     * 操作后的数据（JSON格式）
     */
    @Column(name = "new_data", columnDefinition = "TEXT")
    private String newData;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        CREATE("创建"),
        READ("查询"),
        UPDATE("更新"),
        DELETE("删除"),
        LOGIN("登录"),
        LOGOUT("登出"),
        EXPORT("导出"),
        IMPORT("导入"),
        APPROVE("审批"),
        REJECT("拒绝"),
        ASSIGN("分配"),
        REVOKE("撤销"),
        ENABLE("启用"),
        DISABLE("禁用"),
        RESET("重置"),
        BACKUP("备份"),
        RESTORE("恢复"),
        SYNC("同步"),
        BATCH("批量操作");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        USER("用户"),
        DEVICE("设备"),
        ENTERPRISE("企业"),
        PERMISSION("权限"),
        ROLE("角色"),
        CONFIG("配置"),
        ALERT("告警"),
        LOG("日志"),
        REPORT("报表"),
        DATA("数据"),
        SYSTEM("系统"),
        AUTHENTICATION("认证"),
        AUTHORIZATION("授权");

        private final String description;

        ResourceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 验证状态枚举
     */
    public enum VerificationStatus {
        NONE("无需验证"),
        PENDING("待验证"),
        VERIFIED("已验证"),
        FAILED("验证失败");

        private final String description;

        VerificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查操作是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 检查是否为敏感操作
     */
    public boolean isSensitive() {
        return Boolean.TRUE.equals(isSensitive);
    }

    /**
     * 检查是否需要二次验证
     */
    public boolean needsVerification() {
        return isSensitive() && VerificationStatus.NONE.equals(verificationStatus);
    }

    /**
     * 检查二次验证是否通过
     */
    public boolean isVerified() {
        return VerificationStatus.VERIFIED.equals(verificationStatus);
    }

    /**
     * 获取操作结果描述
     */
    public String getResultDescription() {
        if (isSuccess()) {
            return "操作成功";
        } else {
            return errorMessage != null ? "操作失败: " + errorMessage : "操作失败";
        }
    }

    /**
     * 获取完整操作描述
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(user != null ? user.getUsername() : "未知用户");
        sb.append(" 在 ");
        sb.append(createdAt.toString());
        sb.append(" 执行了");

        if (isSensitive()) {
            sb.append("【敏感】");
        }

        sb.append(operationType.getDescription());
        sb.append(resourceType.getDescription());

        if (resourceName != null) {
            sb.append(" - ").append(resourceName);
        }

        sb.append("，结果：").append(getResultDescription());

        return sb.toString();
    }

    /**
     * 获取用户ID
     */
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
}