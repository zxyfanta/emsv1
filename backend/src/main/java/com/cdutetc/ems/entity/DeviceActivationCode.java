package com.cdutetc.ems.entity;

import com.cdutetc.ems.entity.enums.ActivationCodeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备激活凭证实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ems_device_activation_code")
public class DeviceActivationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 激活码（格式：EMS-RAD-XXXXXXXX）
     */
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    /**
     * 关联的设备
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 激活码状态
     */
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ActivationCodeStatus status = ActivationCodeStatus.UNUSED;

    /**
     * 生成时间
     */
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /**
     * 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 使用时间
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * 使用企业
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_company_id")
    private Company usedByCompany;

    /**
     * 使用用户ID
     */
    @Column(name = "used_by_user_id")
    private Long usedByUserId;

    /**
     * 使用用户名
     */
    @Column(name = "used_by_username", length = 100)
    private String usedByUsername;

    /**
     * 使用IP地址
     */
    @Column(name = "used_ip_address", length = 50)
    private String usedIpAddress;

    /**
     * 备注
     */
    @Column(name = "notes", length = 500)
    private String notes;
}

