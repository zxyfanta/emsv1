package com.ems.common.constants;

/**
 * 告警相关常量
 *
 * @author EMS Team
 */
public final class AlertConstants {

    private AlertConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 告警严重级别枚举
     */
    public enum AlertSeverity {
        LOW("低", "info", "#28a745"),
        MEDIUM("中", "warning", "#ffc107"),
        HIGH("高", "error", "#fd7e14"),
        CRITICAL("严重", "critical", "#dc3545");

        private final String description;
        private final String level;
        private final String color;

        AlertSeverity(String description, String level, String color) {
            this.description = description;
            this.level = level;
            this.color = color;
        }

        public String getDescription() {
            return description;
        }

        public String getLevel() {
            return level;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * 告警状态枚举
     */
    public enum AlertStatus {
        ACTIVE("活跃", "active", "#dc3545"),
        ACKNOWLEDGED("已确认", "acknowledged", "#ffc107"),
        RESOLVED("已解决", "resolved", "#28a745"),
        SUPPRESSED("已抑制", "suppressed", "#6c757d");

        private final String description;
        private final String status;
        private final String color;

        AlertStatus(String description, String status, String color) {
            this.description = description;
            this.status = status;
            this.color = color;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * 告警缓存键前缀
     */
    public static final String ALERT_CACHE_PREFIX = "ems:alert:";

    /**
     * 告警规则缓存键前缀
     */
    public static final String ALERT_RULE_CACHE_PREFIX = "ems:alert:rule:";

    /**
     * 告警缓存过期时间（秒）
     */
    public static final long ALERT_CACHE_EXPIRE_SECONDS = 30 * 60; // 30分钟
    public static final long ALERT_RULE_CACHE_EXPIRE_SECONDS = 60 * 60; // 1小时

    /**
     * 告警消息模板
     */
    public static final String ALERT_MESSAGE_TEMPLATE = "设备 %s 触发%s级别告警：%s";

    /**
     * 默认告警配置
     */
    public static final boolean DEFAULT_ALERT_ENABLED = true;
    public static final int DEFAULT_ALERT_INTERVAL = 300; // 5分钟
    public static final int MAX_ALERT_RECORDS_PER_DEVICE = 1000;
}