package com.ems.common.constants;

/**
 * 用户相关常量
 *
 * @author EMS Team
 */
public final class UserConstants {

    private UserConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        PLATFORM_ADMIN,    // 平台管理员
        ENTERPRISE_ADMIN,  // 企业管理员
        ENTERPRISE_USER    // 企业用户
    }

    /**
     * JWT Token相关常量
     */
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long JWT_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24小时
    public static final String JWT_SECRET = "ems-jwt-secret-key";

    /**
     * 用户缓存键前缀
     */
    public static final String USER_CACHE_PREFIX = "ems:user:";

    /**
     * 用户权限缓存键前缀
     */
    public static final String USER_AUTHORITY_CACHE_PREFIX = "ems:user:authority:";

    /**
     * 用户缓存过期时间（秒）
     */
    public static final long USER_CACHE_EXPIRE_SECONDS = 2 * 60 * 60; // 2小时

    /**
     * 密码加密相关常量
     */
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$";

    /**
     * 用户状态常量
     */
    public static final boolean DEFAULT_USER_ENABLED = true;
    public static final boolean DEFAULT_USER_NON_LOCKED = true;
    public static final boolean DEFAULT_USER_NON_EXPIRED = true;
    public static final boolean DEFAULT_USER_CREDENTIALS_NON_EXPIRED = true;
}