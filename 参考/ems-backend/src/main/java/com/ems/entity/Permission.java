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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 权限实体类
 * 定义系统中的各种权限
 *
 * @author EMS Team
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_code", columnList = "permission_code", unique = true),
    @Index(name = "idx_permission_type", columnList = "permission_type"),
    @Index(name = "idx_resource_type", columnList = "resource_type"),
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_enabled", columnList = "enabled")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 权限编码（唯一标识）
     */
    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    /**
     * 权限名称
     */
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    /**
     * 权限描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 权限类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 50)
    private PermissionType permissionType;

    /**
     * 资源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private ResourceType resourceType;

    /**
     * 资源标识（如菜单ID、API路径等）
     */
    @Column(name = "resource_id", length = 200)
    private String resourceId;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    private ActionType actionType;

    /**
     * 权限级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel;

    /**
     * 父权限ID（用于权限层级）
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Permission parentPermission;

    /**
     * 排序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 权限图标
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * 权限路径（用于面包屑导航）
     */
    @Column(name = "path", length = 500)
    private String path;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 是否为系统权限（不可删除）
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

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
     * 权限类型枚举
     */
    public enum PermissionType {
        MENU("菜单权限"),
        BUTTON("按钮权限"),
        API("API权限"),
        DATA("数据权限");

        private final String description;

        PermissionType(String description) {
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
        SYSTEM("系统管理"),
        USER("用户管理"),
        DEVICE("设备管理"),
        ENTERPRISE("企业管理"),
        ALERT("告警管理"),
        DATA("数据管理"),
        REPORT("报表管理"),
        MONITOR("监控管理");

        private final String description;

        ResourceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 操作类型枚举
     */
    public enum ActionType {
        CREATE("创建"),
        READ("查询"),
        UPDATE("更新"),
        DELETE("删除"),
        EXPORT("导出"),
        IMPORT("导入"),
        APPROVE("审批"),
        EXECUTE("执行");

        private final String description;

        ActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 权限级别枚举
     */
    public enum PermissionLevel {
        GLOBAL("全局权限"),
        ENTERPRISE("企业权限"),
        DEPARTMENT("部门权限"),
        PERSONAL("个人权限");

        private final String description;

        PermissionLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查权限是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 检查是否为系统权限
     */
    public boolean isSystemPermission() {
        return Boolean.TRUE.equals(isSystem);
    }

    /**
     * 检查是否为菜单权限
     */
    public boolean isMenuPermission() {
        return PermissionType.MENU.equals(permissionType);
    }

    /**
     * 检查是否为按钮权限
     */
    public boolean isButtonPermission() {
        return PermissionType.BUTTON.equals(permissionType);
    }

    /**
     * 检查是否为API权限
     */
    public boolean isApiPermission() {
        return PermissionType.API.equals(permissionType);
    }

    /**
     * 检查是否为数据权限
     */
    public boolean isDataPermission() {
        return PermissionType.DATA.equals(permissionType);
    }

    /**
     * 获取完整权限标识
     */
    public String getFullPermissionCode() {
        return String.format("%s:%s:%s:%s",
                resourceType.name(),
                permissionType.name(),
                actionType != null ? actionType.name() : "*",
                resourceId != null ? resourceId : "*");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(id, that.id) || Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, permissionCode);
    }

    @Override
    public String toString() {
        return String.format("Permission{id=%d, code='%s', name='%s', type=%s, resource=%s}",
                id, permissionCode, permissionName, permissionType, resourceType);
    }
}