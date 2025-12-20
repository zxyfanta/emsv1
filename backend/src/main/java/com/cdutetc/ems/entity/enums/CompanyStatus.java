package com.cdutetc.ems.entity.enums;

/**
 * 企业状态枚举
 */
public enum CompanyStatus {
    ACTIVE("活跃"),
    INACTIVE("非活跃");

    private final String displayName;

    CompanyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}