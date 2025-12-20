package com.ems.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis配置类
 * 配置Redis连接、序列化策略和缓存管理
 *
 * @author EMS Team
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * RedisTemplate配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            log.info("🔧 初始化Redis模板...");

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // 配置Jackson序列化器，支持Java 8时间类型
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

            // 使用StringRedisSerializer来序列化和反序列化redis的key值
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(serializer);

            // Hash的key也采用StringRedisSerializer
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(serializer);

            template.afterPropertiesSet();

            log.info("✅ Redis模板初始化成功");
            return template;

        } catch (Exception e) {
            log.error("❌ Redis模板初始化失败", e);
            throw new RuntimeException("Redis配置失败", e);
        }
    }

    /**
     * 字符串专用RedisTemplate（优化性能）
     * 使用不同的Bean名称避免冲突
     */
    @Bean("emsStringRedisTemplate")
    public RedisTemplate<String, String> emsStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // 使用StringRedisSerializer
            StringRedisSerializer serializer = new StringRedisSerializer();
            template.setKeySerializer(serializer);
            template.setValueSerializer(serializer);
            template.setHashKeySerializer(serializer);
            template.setHashValueSerializer(serializer);

            template.afterPropertiesSet();
            log.info("✅ EMS字符串Redis模板初始化成功");
            return template;

        } catch (Exception e) {
            log.error("❌ EMS字符串Redis模板初始化失败", e);
            throw new RuntimeException("EMS字符串Redis配置失败", e);
        }
    }
}