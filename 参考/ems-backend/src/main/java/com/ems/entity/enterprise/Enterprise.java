package com.ems.entity.enterprise;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 企业实体类 - 简化版本
 *
 * @author EMS Team
 */
@Entity
@Table(name = "enterprises", indexes = {
    @Index(name = "idx_enterprise_name", columnList = "name"),
    @Index(name = "idx_enterprise_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class Enterprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 企业名称（唯一）
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

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
}