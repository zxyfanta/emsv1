package com.cdutetc.ems.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 设备缓存同步服务
 *
 * 功能:
 * 1. 实现延迟双删策略,防止并发场景下的脏读
 * 2. 提供立即删除缓存的方法
 * 3. 支持异步延迟删除,不阻塞主流程
 *
 * 延迟双删原理:
 * - T0时刻: 更新MySQL,删除Redis缓存(第一次)
 * - T0+1秒: 再次删除Redis缓存(第二次)
 * - T0~T0+1秒: 并发读取会从MySQL加载最新数据
 * - T0+1秒后: 缓存中数据确保为最新
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheSyncService {

    private final DeviceCacheService deviceCacheService;

    /**
     * 延迟双删: 更新设备后延迟删除缓存
     *
     * 使用场景: 设备信息更新后,防止并发场景下的脏读
     * 执行方式: 异步执行,不阻塞主流程
     * 延迟时间: 1秒
     *
     * @param deviceCode 设备编码
     */
    @Async("cacheSyncExecutor")
    public void evictDeviceWithDelay(String deviceCode) {
        try {
            // 延迟1秒后删除缓存
            Thread.sleep(1000);

            deviceCacheService.evictDevice(deviceCode);

            log.debug("延迟删除设备缓存完成: deviceCode={}", deviceCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("延迟删除设备缓存被中断: deviceCode={}", deviceCode);
        } catch (Exception e) {
            log.error("延迟删除设备缓存失败: deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 立即删除: 用于强制刷新场景
     *
     * 使用场景:
     * - 设备删除时
     * - 设备状态更新时
     * - 需要立即生效的其他场景
     *
     * @param deviceCode 设备编码
     */
    public void evictDeviceImmediate(String deviceCode) {
        deviceCacheService.evictDevice(deviceCode);
        log.debug("立即删除设备缓存: deviceCode={}", deviceCode);
    }
}
