package com.ems.entity.device;

import jakarta.persistence.*;
import com.ems.entity.enterprise.Enterprise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 设备分组实体
 * 用于组织和管理设备的逻辑分组
 *
 * @author EMS Team
 */
@Entity
@Table(name = "device_groups")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private DeviceGroup parentGroup;

    @OneToMany(mappedBy = "parentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DeviceGroup> childGroups = new HashSet<>();

    // 注意：设备关联通过DeviceGroupMapping实体管理，避免直接JPA映射问题

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id")
    private Enterprise enterprise;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GroupType groupType = GroupType.CUSTOM;

    @Column(length = 20)
    private String color;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * 设备分组类型枚举
     */
    public enum GroupType {
        SYSTEM("系统分组"),
        CUSTOM("自定义分组"),
        LOCATION("位置分组"),
        FUNCTION("功能分组");

        private final String description;

        GroupType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查分组是否为根分组
     */
    public boolean isRootGroup() {
        return parentGroup == null;
    }

    /**
     * 检查分组是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 添加子分组
     */
    public void addChildGroup(DeviceGroup childGroup) {
        if (childGroups == null) {
            childGroups = new HashSet<>();
        }
        childGroup.setParentGroup(this);
        childGroups.add(childGroup);
    }

    /**
     * 移除子分组
     */
    public void removeChildGroup(DeviceGroup childGroup) {
        if (childGroups != null) {
            childGroup.setParentGroup(null);
            childGroups.remove(childGroup);
        }
    }

    /**
     * 检查分组是否为系统类型
     *
     * @return true if group is system type, false otherwise
     */
    public boolean isSystemGroup() {
        return GroupType.SYSTEM.equals(groupType);
    }

    /**
     * 检查分组是否为自定义类型
     *
     * @return true if group is custom type, false otherwise
     */
    public boolean isCustomGroup() {
        return GroupType.CUSTOM.equals(groupType);
    }

    
    /**
     * 获取所有设备数量（包括子分组的设备）
     * 注意：此方法需要在Service层通过DeviceGroupMapping实现
     */
    public int getTotalDeviceCount() {
        // 实际实现由Service层通过DeviceGroupMappingRepository完成
        // 这里返回0避免编译错误
        int count = 0;
        if (childGroups != null) {
            for (DeviceGroup childGroup : childGroups) {
                count += childGroup.getTotalDeviceCount();
            }
        }
        return count;
    }

    /**
     * 获取层级路径
     */
    public String getGroupPath() {
        if (isRootGroup()) {
            return "/" + name;
        } else {
            String parentPath = parentGroup != null ? parentGroup.getGroupPath() : "";
            return parentPath + "/" + name;
        }
    }
}