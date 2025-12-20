package com.ems.entity.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ems.entity.enterprise.Enterprise;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MQTT设备实体类 - 简化版本
 *
 * @author EMS Team
 */
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_device_device_id", columnList = "device_id"),
    @Index(name = "idx_device_enterprise_id", columnList = "enterprise_id"),
    @Index(name = "idx_device_status", columnList = "status"),
    @Index(name = "idx_device_device_type", columnList = "device_type"),
    @Index(name = "idx_device_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 企业ID
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    /**
     * 设备唯一编号（全局唯一）
     */
    @Column(name = "device_id", nullable = false, unique = true, length = 50)
    private String deviceId;

    /**
     * 设备名称
     */
    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    /**
     * 设备状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.OFFLINE;

    /**
     * 设备类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 50)
    @Builder.Default
    private DeviceType deviceType = DeviceType.RADIATION;

    /**
     * 最后在线时间
     */
    @Column(name = "last_online_at")
    private LocalDateTime lastOnlineAt;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 是否删除（软删除）
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // 注意：设备分组通过DeviceGroupMapping实体管理，避免直接JPA映射问题

    /**
     * 检查设备是否在线
     *
     * @return true if device is online, false otherwise
     */
    public boolean isOnline() {
        return DeviceStatus.ONLINE.equals(status);
    }

    /**
     * 检查设备是否为辐射监测仪
     *
     * @return true if device is radiation monitor, false otherwise
     */
    public boolean isRadiationDevice() {
        return DeviceType.RADIATION.equals(deviceType);
    }

    /**
     * 检查设备是否为环境监测站
     *
     * @return true if device is environment monitor, false otherwise
     */
    public boolean isEnvironmentDevice() {
        return DeviceType.ENVIRONMENT.equals(deviceType);
    }

    /**
     * 设备状态枚举 - 简化版本
     */
    public enum DeviceStatus {
        ONLINE("在线"),
        OFFLINE("离线"),
        UNKNOWN("未知");

        private final String description;

        DeviceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        RADIATION("辐射监测仪"),
        ENVIRONMENT("环境监测站");

        private final String description;

        DeviceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}