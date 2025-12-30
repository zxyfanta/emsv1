package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Alert;
import com.cdutetc.ems.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 告警信息缓存服务
 *
 * 功能:
 * 1. 缓存设备的未解决告警列表,减少数据库查询
 * 2. 提供缓存查询、更新、失效方法
 * 3. TTL设置10分钟,平衡性能和数据新鲜度
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AlertRepository alertRepository;

    private static final String CACHE_PREFIX = "alert:device:";
    private static final long CACHE_TTL_MINUTES = 5;  // 缩短至5分钟,与设备缓存TTL保持一致

    /**
     * 获取设备的未解决告警列表(带缓存)
     *
     * 缓存策略: Cache Aside
     * 1. 先从Redis获取
     * 2. 未命中则从数据库加载并写入Redis
     * 3. TTL 10分钟
     *
     * @param deviceId 设备ID
     * @return 未解决告警列表
     */
    public List<Alert> getUnresolvedAlerts(Long deviceId) {
        String key = CACHE_PREFIX + deviceId;

        try {
            // 1. 从Redis获取
            List<Alert> cached = (List<Alert>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("告警缓存命中: deviceId={}", deviceId);
                return cached;
            }

            // 2. 从数据库加载
            log.debug("告警缓存未命中,从数据库加载: deviceId={}", deviceId);
            List<Alert> alerts = alertRepository.findByDeviceId(deviceId)
                .stream()
                .filter(a -> !a.getResolved())
                .toList();

            // 3. 缓存(只缓存有告警的情况)
            if (!alerts.isEmpty()) {
                redisTemplate.opsForValue().set(key, alerts, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("未解决告警已缓存: deviceId={}, count={}", deviceId, alerts.size());
            } else {
                log.debug("设备无未解决告警: deviceId={}", deviceId);
            }

            return alerts;
        } catch (Exception e) {
            log.error("获取告警缓存异常: deviceId={}, 降级到数据库查询", deviceId, e);
            // 降级: 直接查询数据库
            return alertRepository.findByDeviceId(deviceId)
                .stream()
                .filter(a -> !a.getResolved())
                .toList();
        }
    }

    /**
     * 清除设备告警缓存
     *
     * 使用场景: 告警解决、创建新告警时调用
     *
     * @param deviceId 设备ID
     */
    public void evictDeviceAlerts(Long deviceId) {
        String key = CACHE_PREFIX + deviceId;

        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("清除设备告警缓存: deviceId={}, 结果: {}", deviceId, deleted);
        } catch (Exception e) {
            log.error("清除设备告警缓存失败: deviceId={}", deviceId, e);
        }
    }
}
