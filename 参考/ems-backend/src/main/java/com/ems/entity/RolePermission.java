package com.ems.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 角色权限关联实体类
 * 用于管理角色与权限的多对多关系
 *
 * @author EMS Team
 */
@Entity
@Table(name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}),
    indexes = {
        @Index(name = "idx_role_permission_role", columnList = "role_id"),
        @Index(name = "idx_role_permission_permission", columnList = "permission_id"),
        @Index(name = "idx_role_permission_enabled", columnList = "enabled")
    })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的角色
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private User.UserRole role;

    /**
     * 关联的权限
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 权限限制条件（JSON格式，用于数据权限的过滤条件）
     */
    @Column(name = "condition", columnDefinition = "TEXT")
    private String condition;

    /**
     * 权限范围
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 50)
    private PermissionScope scope;

    /**
     * 权限优先级
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * 过期时间（null表示永不过期）
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 权限范围枚举
     */
    public enum PermissionScope {
        ALL("全部"),
        SELF("仅自己"),
        DEPARTMENT("部门"),
        ENTERPRISE("企业"),
        CUSTOM("自定义");

        private final String description;

        PermissionScope(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查关联是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 检查权限是否过期
     */
    public boolean isExpired() {
        return expiredAt != null && expiredAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查权限是否有效
     */
    public boolean isValid() {
        return isEnabled() && !isExpired();
    }

    /**
     * 获取角色ID
     */
    public Long getRoleId() {
        return role != null ? role.ordinal() + 1L : null;
    }

    /**
     * 获取权限ID
     */
    public Long getPermissionId() {
        return permission != null ? permission.getId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermission that = (RolePermission) o;
        return Objects.equals(id, that.id) ||
               (Objects.equals(role, that.role) && Objects.equals(permission, that.permission));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, permission);
    }

    @Override
    public String toString() {
        return String.format("RolePermission{id=%d, role=%s, permission='%s', enabled=%s}",
                id, role, permission != null ? permission.getPermissionCode() : null, enabled);
    }
}