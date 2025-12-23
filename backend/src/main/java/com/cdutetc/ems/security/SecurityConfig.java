package com.cdutetc.ems.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许所有源
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));

        // 允许发送Cookie
        configuration.setAllowCredentials(true);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));

        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(AbstractHttpConfigurer::disable)

            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 配置会话管理为无状态
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置异常处理
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许访问的端点 - 按优先级排序
                .requestMatchers("/device-data/**").permitAll()  // 设备数据接收API免认证 (最优先)
                .requestMatchers("/auth/**").permitAll()  // 认证API免认证 (注意：context-path是/api，实际路径是/api/auth/**)
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()  // 健康检查和监控端点免认证
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 允许错误处理器访问
                .requestMatchers("/error").permitAll()
                // 系统配置API免认证（前端需要获取功能开关配置）
                .requestMatchers("/system-config").permitAll()

                // 管理员权限
                .requestMatchers(HttpMethod.POST, "/api/companies").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/companies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/companies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // 其他请求需要认证
                .anyRequest().authenticated()
            );

        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 允许H2控制台的iframe
        http.headers(headers -> headers.frameOptions().disable());

        return http.build();
    }
}