package com.ems.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Optional;

/**
 * 安全工具类
 *
 * @author EMS Team
 */
public class SecurityUtils {

    /**
     * 获取当前认证的用户信息
     */
    public static Optional<UserDetails> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return Optional.of((UserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            // 获取第一个权限，并去除ROLE_前缀
            String authority = authentication.getAuthorities().iterator().next().getAuthority();
            if (authority.startsWith("ROLE_")) {
                return authority.substring(5);
            }
            return authority;
        }
        return null;
    }

    /**
     * 获取当前用户的企业ID
     * 注意：这里需要根据实际的JWT Claims或用户信息来获取企业ID
     */
    public static Long getCurrentUserEnterpriseId() {
        // 暂时返回null，实际项目中应该从JWT token或用户信息中获取
        // 这里可能需要自定义UserDetails或从JWT Claims中提取
        return 1L; // 临时返回企业ID为1，用于测试
    }

    /**
     * 检查当前用户是否具有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            return authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }

    /**
     * 检查当前用户是否为平台管理员
     */
    public static boolean isPlatformAdmin() {
        return hasRole("PLATFORM_ADMIN");
    }

    /**
     * 检查当前用户是否为企业管理员
     */
    public static boolean isEnterpriseAdmin() {
        return hasRole("ENTERPRISE_ADMIN");
    }

    /**
     * 检查当前用户是否为企业用户
     */
    public static boolean isEnterpriseUser() {
        return hasRole("ENTERPRISE_USER");
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getPrincipal());
    }
}