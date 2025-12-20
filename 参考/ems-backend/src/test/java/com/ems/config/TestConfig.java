package com.ems.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 测试配置类
 * 提供测试环境专用的Bean配置
 *
 * @author EMS Team
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * 测试环境的密码编码器
     *
     * @return BCryptPasswordEncoder实例
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}