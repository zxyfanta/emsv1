package com.ems.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger API文档配置
 *
 * @author EMS Team
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EMS企业级MQTT设备管理云平台 API")
                        .description("提供完整的设备管理、数据处理、监控告警、用户认证等功能的RESTful API。基于Spring Boot 3和Spring Security 6构建，支持JWT认证。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EMS开发团队")
                                .email("ems@example.com")
                                .url("https://github.com/ems-platform"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://api.ems.com/api")
                                .description("生产环境")
                ))
                .tags(List.of(
                        new Tag()
                                .name("认证管理")
                                .description("用户注册、登录、权限验证等认证相关接口"),
                        new Tag()
                                .name("企业管理")
                                .description("企业信息的增删改查、统计等管理接口"),
                        new Tag()
                                .name("设备管理")
                                .description("设备信息的增删改查、状态管理等接口"),
                        new Tag()
                                .name("设备分组")
                                .description("设备分组的创建、管理和设备关联接口"),
                        new Tag()
                                .name("告警管理")
                                .description("告警规则配置、告警记录查询等接口"),
                        new Tag()
                                .name("数据导出")
                                .description("设备数据导出、报告生成等接口"),
                        new Tag()
                                .name("系统监控")
                                .description("系统状态监控、健康检查等接口")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("JWT认证Token，格式：Bearer {token}")));
    }
}