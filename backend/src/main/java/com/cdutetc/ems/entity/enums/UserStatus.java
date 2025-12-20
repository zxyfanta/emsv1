package com.cdutetc.ems.entity.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /**
     * 激活状态
     */
    ACTIVE("激活"),

    /**
     * 非活跃状态
     */
    INACTIVE("非活跃");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}