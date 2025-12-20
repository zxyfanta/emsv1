package com.ems;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EMS企业级MQTT设备管理云平台 - 应用启动类
 *
 * @author EMS Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.ems.repository")
@EnableAsync
@EnableScheduling
public class EMSApplication {

    public static void main(String[] args) {
        SpringApplication.run(EMSApplication.class, args);
        log.info("==========================================");
        log.info("🚀 EMS企业级MQTT设备管理云平台启动成功!");
        log.info("📊 访问地址: http://localhost:8080");
        log.info("📚 API文档: http://localhost:8080/swagger-ui.html");
        log.info("🏥 健康检查: http://localhost:8080/actuator/health");
        log.info("==========================================");
    }
}