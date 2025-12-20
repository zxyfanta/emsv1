package com.cdutetc.ems.entity.enums;

/**
 * 用户角色枚举
 */
public enum UserRole {
    /**
     * 管理员
     */
    ADMIN("管理员"),

    /**
     * 普通用户
     */
    USER("普通用户");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}