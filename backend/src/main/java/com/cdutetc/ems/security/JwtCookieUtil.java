package com.cdutetc.ems.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT Cookie 工具类
 * 用于设置和管理 JWT Cookie
 */
@Slf4j
@Component
public class JwtCookieUtil {

    /**
     * Cookie 名称
     */
    public static final String JWT_COOKIE_NAME = "jwt_token";

    /**
     * Cookie 最大年龄（7天）
     */
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    /**
     * 将 JWT token 添加到 HttpOnly Cookie
     *
     * @param token JWT token
     * @param response HTTP 响应
     */
    public void addJwtCookie(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);

        // 设置 Cookie 属性
        cookie.setHttpOnly(true);      // 防止 XSS 攻击
        cookie.setSecure(false);         // 开发环境设为 false，生产环境应设为 true（需要 HTTPS）
        cookie.setPath("/");            // 对所有路径有效
        cookie.setMaxAge(COOKIE_MAX_AGE); // 7天有效期

        // 设置 SameSite 属性（通过响应头，Jakarta Cookie 不支持 setSameSite 方法）
        response.setHeader("Set-Cookie",
            String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                JWT_COOKIE_NAME,
                token,
                COOKIE_MAX_AGE));

        log.debug("JWT token 已添加到 Cookie (HttpOnly, SameSite=Lax)");
    }

    /**
     * 清除 JWT Cookie
     *
     * @param response HTTP 响应
     */
    public void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 立即过期
        response.addCookie(cookie);
        log.debug("JWT Cookie 已清除");
    }

    /**
     * 从 Cookie 数组中提取 JWT token
     *
     * @param cookies Cookie 数组
     * @return JWT token，如果不存在则返回 null
     */
    public String extractJwtFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                String token = cookie.getValue();
                log.debug("从 Cookie 中提取到 JWT token");
                return token;
            }
        }

        return null;
    }
}
