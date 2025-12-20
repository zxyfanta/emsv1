package com.cdutetc.ems.entity.enums;

/**
 * 设备类型枚举
 */
public enum DeviceType {
    RADIATION_MONITOR("辐射监测仪", "RADIATION_MONITOR"),
    ENVIRONMENT_STATION("环境监测站", "ENVIRONMENT_STATION");

    private final String displayName;
    private final String code;

    DeviceType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}