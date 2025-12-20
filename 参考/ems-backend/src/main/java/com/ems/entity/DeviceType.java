package com.ems.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 设备类型实体类
 * 定义不同类型的设备及其配置信息
 *
 * @author EMS Team
 * @version 1.0.0
 */
@Entity
@Table(name = "device_types", indexes = {
    @Index(name = "idx_device_type_code", columnList = "type_code"),
    @Index(name = "idx_device_type_enabled", columnList = "enabled"),
    @Index(name = "idx_device_type_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class DeviceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设备类型代码（唯一标识）
     */
    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode;

    /**
     * 设备类型显示名称
     */
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    /**
     * 数据表名称
     */
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * MQTT主题模式
     */
    @Column(name = "mqtt_topic_pattern", length = 500)
    private String mqttTopicPattern;

    /**
     * 设备类型描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
     * 设备类型枚举定义
     */
    public enum Type {
        RADIATION("辐射监测仪", "RADIATION", "radiation_device_status", "ems/device/+/data/RADIATION", true),
        ENVIRONMENT("环境监测站", "ENVIRONMENT", "environment_device_status", "ems/device/+/data/ENVIRONMENT", true);

        private final String displayName;
        private final String code;
        private final String tableName;
        private final String mqttTopicPattern;
        private final boolean defaultEnabled;

        Type(String displayName, String code, String tableName, String mqttTopicPattern, boolean defaultEnabled) {
            this.displayName = displayName;
            this.code = code;
            this.tableName = tableName;
            this.mqttTopicPattern = mqttTopicPattern;
            this.defaultEnabled = defaultEnabled;
        }

        public String getDisplayName() { return displayName; }
        public String getCode() { return code; }
        public String getTableName() { return tableName; }
        public String getMqttTopicPattern() { return mqttTopicPattern; }
        public boolean isDefaultEnabled() { return defaultEnabled; }

        public static Type fromCode(String code) {
            for (Type type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的设备类型代码: " + code);
        }
    }

    // 业务方法

    /**
     * 获取设备类型枚举
     */
    public Type getTypeEnum() {
        return Type.fromCode(this.typeCode);
    }

    /**
     * 检查MQTT主题是否匹配
     */
    public boolean matchesTopic(String topic) {
        if (mqttTopicPattern == null || mqttTopicPattern.isEmpty()) {
            return false;
        }

        // 简单的通配符匹配
        String pattern = mqttTopicPattern.replace("+", "[^/]+").replace("#", ".*");
        return topic.matches(pattern);
    }

    /**
     * 从MQTT主题中提取设备类型
     * 支持方案一主题结构: ems/device/{deviceId}/data/{deviceType}
     */
    public static String extractDeviceTypeFromTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }

        // 解析主题: ems/device/{deviceId}/data/{deviceType}
        String[] parts = topic.split("/");
        if (parts.length == 5 &&
            "ems".equals(parts[0]) &&
            "device".equals(parts[1]) &&
            "data".equals(parts[3])) {
            return parts[4]; // 设备类型在最后部分
        }

        return null;
    }

    /**
     * 从MQTT主题中提取设备ID
     * 支持方案一主题结构: ems/device/{deviceId}/data/{deviceType}
     */
    public static String extractDeviceIdFromTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }

        // 解析主题: ems/device/{deviceId}/data/{deviceType}
        String[] parts = topic.split("/");
        if (parts.length == 5 &&
            "ems".equals(parts[0]) &&
            "device".equals(parts[1]) &&
            "data".equals(parts[3])) {
            return parts[2]; // 设备ID在第三部分
        }

        return null;
    }

    /**
     * 验证主题格式是否符合方案一: ems/device/{deviceId}/data/{deviceType}
     */
    public static boolean isValidTopicFormat(String topic) {
        return extractDeviceTypeFromTopic(topic) != null &&
               extractDeviceIdFromTopic(topic) != null;
    }

    /**
     * 获取设备类型的完整描述
     */
    public String getFullDescription() {
        return String.format("%s (%s) - %s", typeName, typeCode,
                             description != null ? description : "无描述");
    }

    // 重写equals和hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceType that = (DeviceType) o;
        return Objects.equals(typeCode, that.typeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeCode);
    }

    @Override
    public String toString() {
        return String.format("DeviceType{id=%d, code='%s', name='%s', enabled=%s}",
                           id, typeCode, typeName, enabled);
    }
}