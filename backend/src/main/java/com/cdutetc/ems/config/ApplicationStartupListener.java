package com.cdutetc.ems.config;

import com.cdutetc.ems.service.DeviceStatusCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器
 * 用于在应用启动完成后执行初始化任务
 *
 * @author EMS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupListener {

    private final DeviceStatusCacheService deviceStatusCacheService;

    /**
     * 应用启动完成后执行
     * 用于预热设备状态缓存
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========================================");
        log.info("应用启动完成，开始执行初始化任务...");

        try {
            // 预热设备状态缓存
            deviceStatusCacheService.warmUpCache();
            log.info("设备状态缓存预热完成");
        } catch (Exception e) {
            log.error("应用初始化任务执行失败", e);
        }

        log.info("应用初始化任务执行完毕");
        log.info("========================================");
    }
}
