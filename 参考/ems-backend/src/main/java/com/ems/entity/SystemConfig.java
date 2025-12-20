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

/**
 * 系统配置实体类
 * 用于存储系统的动态配置信息
 *
 * @author EMS Team
 */
@Entity
@Table(name = "system_configs", indexes = {
    @Index(name = "idx_config_category", columnList = "category"),
    @Index(name = "idx_config_key", columnList = "config_key", unique = true),
    @Index(name = "idx_config_enabled", columnList = "enabled"),
    @Index(name = "idx_config_updated_at", columnList = "updated_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键（唯一标识）
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置显示名称
     */
    @Column(name = "config_name", nullable = false, length = 200)
    private String configName;

    /**
     * 配置描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 配置分类
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ConfigCategory category;

    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false, length = 20)
    private ConfigType configType;

    /**
     * 默认值
     */
    @Column(name = "default_value", length = 1000)
    private String defaultValue;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 是否为系统配置（不可删除）
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    /**
     * 是否需要重启生效
     */
    @Column(name = "require_restart", nullable = false)
    @Builder.Default
    private Boolean requireRestart = false;

    /**
     * 配置选项（JSON格式，用于下拉选择等）
     */
    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    /**
     * 校验规则（正则表达式等）
     */
    @Column(name = "validation_rule", length = 500)
    private String validationRule;

    /**
     * 最后修改者
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedByUser;

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
     * 配置分类枚举
     */
    public enum ConfigCategory {
        SYSTEM("系统配置"),
        MQTT("MQTT配置"),
        DATABASE("数据库配置"),
        REDIS("Redis配置"),
        SECURITY("安全配置"),
        EMAIL("邮件配置"),
        SMS("短信配置"),
        ALERT("告警配置"),
        MONITOR("监控配置"),
        BUSINESS("业务配置");

        private final String description;

        ConfigCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 配置类型枚举
     */
    public enum ConfigType {
        STRING("字符串"),
        NUMBER("数字"),
        BOOLEAN("布尔值"),
        JSON("JSON对象"),
        ARRAY("数组"),
        PASSWORD("密码"),
        EMAIL("邮箱"),
        URL("网址"),
        TEXT("长文本");

        private final String description;

        ConfigType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取当前配置值（如果当前值为空则返回默认值）
     */
    public String getValueOrDefault() {
        return configValue != null ? configValue : defaultValue;
    }

    /**
     * 检查配置是否为字符串类型
     */
    public boolean isStringType() {
        return ConfigType.STRING.equals(configType) ||
               ConfigType.EMAIL.equals(configType) ||
               ConfigType.URL.equals(configType) ||
               ConfigType.PASSWORD.equals(configType);
    }

    /**
     * 检查配置是否为数字类型
     */
    public boolean isNumberType() {
        return ConfigType.NUMBER.equals(configType);
    }

    /**
     * 检查配置是否为布尔类型
     */
    public boolean isBooleanType() {
        return ConfigType.BOOLEAN.equals(configType);
    }

    /**
     * 检查配置是否为JSON类型
     */
    public boolean isJsonType() {
        return ConfigType.JSON.equals(configType) || ConfigType.ARRAY.equals(configType);
    }

    /**
     * 获取布尔值
     */
    public Boolean getBooleanValue() {
        if (!isBooleanType()) {
            throw new UnsupportedOperationException("配置类型不是布尔值");
        }
        String value = getValueOrDefault();
        if (value == null) return null;
        return Boolean.valueOf(value);
    }

    /**
     * 获取数字值
     */
    public Number getNumericValue() {
        if (!isNumberType()) {
            throw new UnsupportedOperationException("配置类型不是数字");
        }
        String value = getValueOrDefault();
        if (value == null) return null;

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("配置值不是有效的数字: " + value);
        }
    }

    /**
     * 设置布尔值
     */
    public void setBooleanValue(Boolean value) {
        if (!isBooleanType()) {
            throw new UnsupportedOperationException("配置类型不是布尔值");
        }
        this.configValue = value != null ? value.toString() : null;
    }

    /**
     * 设置数字值
     */
    public void setNumericValue(Number value) {
        if (!isNumberType()) {
            throw new UnsupportedOperationException("配置类型不是数字");
        }
        this.configValue = value != null ? value.toString() : null;
    }

    /**
     * 检查配置是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 检查是否为系统配置
     */
    public boolean isSystemConfig() {
        return Boolean.TRUE.equals(isSystem);
    }

    /**
     * 检查是否需要重启生效
     */
    public boolean isRequireRestart() {
        return Boolean.TRUE.equals(requireRestart);
    }
}