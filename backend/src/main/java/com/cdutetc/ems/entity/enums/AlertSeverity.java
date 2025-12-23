package com.cdutetc.ems.entity.enums;

/**
 * 告警严重程度枚举
 */
public enum AlertSeverity {
    /**
     * 严重告警 - 需要立即处理
     */
    CRITICAL("CRITICAL", "严重"),

    /**
     * 警告 - 需要关注
     */
    WARNING("WARNING", "警告"),

    /**
     * 信息 - 提示
     */
    INFO("INFO", "信息");

    private final String code;
    private final String description;

    AlertSeverity(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static AlertSeverity fromCode(String code) {
        for (AlertSeverity severity : values()) {
            if (severity.code.equals(code)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("未知严重程度: " + code);
    }
}
