package com.cdutetc.ems.config;

import com.cdutetc.ems.service.DeviceCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 缓存预热启动器
 *
 * 功能: 应用启动完成后自动预热设备缓存
 * 时机: 使用ApplicationRunner确保在应用完全启动后执行
 * 优先级: @Order(1) 确保在其他初始化逻辑之前执行
 *
 * @author EMS Team
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CacheWarmUpRunner implements ApplicationRunner {

    private final DeviceCacheService deviceCacheService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("开始执行缓存预热...");
        log.info("========================================");

        try {
            deviceCacheService.warmUpCache();
            log.info("缓存预热成功完成!");
        } catch (Exception e) {
            log.error("缓存预热失败,但不影响应用启动", e);
        }

        log.info("========================================");
    }
}
