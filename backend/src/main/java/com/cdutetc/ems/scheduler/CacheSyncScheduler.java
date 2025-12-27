package com.cdutetc.ems.scheduler;

import com.cdutetc.ems.service.DeviceStatusCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存同步定时任务
 *
 * 功能：
 * 1. 定时将Redis中的设备状态同步到数据库
 * 2. 保持数据持久化，防止Redis数据丢失
 *
 * @author EMS Team
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CacheSyncScheduler {

    private final DeviceStatusCacheService deviceStatusCacheService;

    /**
     * 每5分钟同步一次设备状态缓存到数据库
     * initialDelay: 启动后延迟30秒开始第一次执行
     * fixedRate: 每5分钟执行一次（300000毫秒）
     */
    @Scheduled(initialDelay = 30000, fixedRate = 300000)
    public void syncDeviceStatusToDatabase() {
        log.debug("开始执行设备状态缓存同步任务...");
        try {
            deviceStatusCacheService.flushToDatabase();
        } catch (Exception e) {
            log.error("设备状态缓存同步任务执行失败", e);
        }
    }
}
