package com.ems.config;

import com.ems.security.auth.JwtAuthenticationFilter;
import com.ems.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 *
 * @author EMS Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 配置密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 配置认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        // 允许的头部
        configuration.setAllowedHeaders(List.of("*"));

        // 允许凭证
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（JWT不需要）
            .csrf(AbstractHttpConfigurer::disable)

            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 配置会话管理为无状态
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置认证提供者
            .authenticationProvider(authenticationProvider())

            // 添加JWT过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers(
                    "/auth/**",
                    "/public/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/swagger-ui.html",
                    "/api/actuator/**",
                    "/actuator/**",
                    "/api/health/**",
                    "/health/**",
                    "/error"
                ).permitAll()

                // 平台管理员权限
                .requestMatchers(
                    "/platform-admin/**"
                ).hasRole("PLATFORM_ADMIN")

                // 企业管理员权限
                .requestMatchers(
                    "/enterprise-admin/**"
                ).hasAnyRole("PLATFORM_ADMIN", "ENTERPRISE_ADMIN")

                // 设备管理权限
                .requestMatchers(
                    "/devices/**"
                ).hasAnyRole("PLATFORM_ADMIN", "ENTERPRISE_ADMIN", "ENTERPRISE_USER")

                // 设备类型管理权限
                .requestMatchers(
                    "/device-types/**"
                ).hasAnyRole("PLATFORM_ADMIN", "ENTERPRISE_ADMIN")

                // 企业管理权限
                .requestMatchers(
                    "/enterprises/**"
                ).hasAnyRole("PLATFORM_ADMIN", "ENTERPRISE_ADMIN")

                // 告警管理权限
                .requestMatchers(
                    "/alerts/**"
                ).hasAnyRole("PLATFORM_ADMIN", "ENTERPRISE_ADMIN", "ENTERPRISE_USER")

                // 其他接口需要认证
                .anyRequest().authenticated()
            )

            // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(401);
                    response.getWriter().write("""
                        {
                            "code": 401,
                            "message": "未认证，请先登录",
                            "data": null,
                            "timestamp": "%s"
                        }
                        """.formatted(java.time.LocalDateTime.now())
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(403);
                    response.getWriter().write("""
                        {
                            "code": 403,
                            "message": "权限不足，无法访问",
                            "data": null,
                            "timestamp": "%s"
                        }
                        """.formatted(java.time.LocalDateTime.now())
                    );
                })
            );

        return http.build();
    }
}