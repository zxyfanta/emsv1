package com.cdutetc.ems.entity.enums;

/**
 * 设备状态枚举
 */
public enum DeviceStatus {
    ONLINE("在线"),
    OFFLINE("离线"),
    MAINTENANCE("维护中"),
    FAULT("故障");

    private final String displayName;

    DeviceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}