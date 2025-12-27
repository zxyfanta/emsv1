package com.cdutetc.ems.entity.enums;

/**
 * 告警类型枚举
 */
public enum AlertType {
    /**
     * CPM突增告警（基于上升率）
     */
    CPM_RISE("CPM_RISE", "辐射值突增"),

    /**
     * 低电量告警
     */
    LOW_BATTERY("LOW_BATTERY", "电量不足"),

    /**
     * 设备离线告警（基于超时无数据）
     */
    OFFLINE("OFFLINE", "设备离线");

    private final String code;
    private final String description;

    AlertType(String code, String description) {
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
    public static AlertType fromCode(String code) {
        for (AlertType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知告警类型: " + code);
    }
}
