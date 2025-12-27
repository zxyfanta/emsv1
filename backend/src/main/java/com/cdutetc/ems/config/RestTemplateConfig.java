package com.cdutetc.ems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置
 * 用于数据上报 HTTP 请求
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 连接超时：10秒
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());

        // 读取超时：30秒
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

        return new RestTemplate(factory);
    }
}
