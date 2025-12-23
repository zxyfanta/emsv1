package com.cdutetc.ems.entity.enums;

/**
 * 告警类型枚举
 */
public enum AlertType {
    /**
     * 高辐射值告警
     */
    HIGH_CPM("HIGH_CPM", "辐射值超标"),

    /**
     * 设备离线告警
     */
    OFFLINE("OFFLINE", "设备离线"),

    /**
     * 设备故障告警
     */
    FAULT("FAULT", "设备故障"),

    /**
     * 低电量告警
     */
    LOW_BATTERY("LOW_BATTERY", "电量不足");

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
