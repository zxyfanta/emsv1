package com.ems.entity.device;

import jakarta.persistence.*;
import com.ems.entity.enterprise.Enterprise;
import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 设备分组映射实体
 * 用于管理设备与分组的多对多关系
 *
 * @author EMS Team
 */
@Entity
@Table(name = "device_group_mappings",
    indexes = {
        @Index(name = "idx_mapping_device_id", columnList = "device_id"),
        @Index(name = "idx_mapping_group_id", columnList = "group_id"),
        @Index(name = "idx_mapping_enterprise_id", columnList = "enterprise_id"),
        @Index(name = "idx_mapping_unique", columnList = "device_id,group_id", unique = true)
    })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceGroupMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的设备
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 关联的设备分组
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private DeviceGroup deviceGroup;

    /**
     * 关联的企业
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 检查映射是否启用
     *
     * @return true if mapping is enabled, false otherwise
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}