package com.cdutetc.ems.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 支持从多个来源读取 JWT token：
 * 1. Authorization 头（常规 API 调用）
 * 2. Cookie（SSE 连接，标准做法）
 * 3. URL 参数（临时方案，不推荐）
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtCookieUtil jwtCookieUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String username = null;
        String jwtToken = null;

        // 1. 首先尝试从 Authorization 头获取 token（常规 API）
        final String requestTokenHeader = request.getHeader("Authorization");
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
                log.debug("Found JWT token in Authorization header");
            } catch (Exception e) {
                log.warn("Unable to get JWT Token from header: {}", e.getMessage());
            }
        }
        // 2. 从 Cookie 获取 token（SSE 标准做法）
        else if ((jwtToken = jwtCookieUtil.extractJwtFromCookies(request.getCookies())) != null) {
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
                log.debug("Found JWT token in Cookie");
            } catch (Exception e) {
                log.warn("Unable to get JWT Token from Cookie: {}", e.getMessage());
            }
        }
        // 3. 最后尝试从 URL 参数获取（临时方案，不推荐）
        else if (request.getParameter("token") != null) {
            jwtToken = request.getParameter("token");
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
                log.warn("Found JWT token in URL parameter (not recommended)");
            } catch (Exception e) {
                log.warn("Unable to get JWT Token from URL parameter: {}", e.getMessage());
            }
        } else {
            log.debug("JWT Token not found in header, cookie or URL parameter");
        }

        // 验证token
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 如果token有效，配置Spring Security手动设置认证
            if (jwtUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置认证信息到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("User '{}' authenticated successfully", username);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 跳过认证的路径
        return path.startsWith("/api/auth/") ||
               path.startsWith("/h2-console") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/api/device-data/") ||  // 设备数据接收API免认证
               path.equals("/api/system-config") ||     // 系统配置API免认证
               path.equals("/api/error") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/swagger-resources");
    }
}