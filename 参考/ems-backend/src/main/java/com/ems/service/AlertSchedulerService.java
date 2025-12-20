package com.ems.service;

import com.ems.entity.device.Device;
import com.ems.repository.AlertRecordRepository;
import com.ems.repository.device.DeviceRepository;
import com.ems.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警定时任务服务
 * 负责定时检查设备数据并触发告警检测
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSchedulerService {

    private final AlertService alertService;
    private final DeviceRepository deviceRepository;
    private final RedisCacheService redisCacheService;
    private final AlertRecordRepository alertRecordRepository;

    /**
     * 每分钟定时检查告警
     * 按需求文档要求：检查频率每分钟一次
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduleAlertCheck() {
        try {
            log.debug("开始定时告警检查");

            // 获取所有设备（包括离线设备，以确保所有设备的告警都被检查）
            List<Device> devices = getUserAccessibleDevices();

            int checkedCount = 0;

            for (Device device : devices) {
                try {
                    // 从Redis获取最新实时数据
                    checkDeviceAlerts(device);
                    checkedCount++;

                } catch (Exception e) {
                    log.warn("检查设备告警失败: 设备={}", device.getDeviceId(), e);
                }
            }

            log.debug("定时告警检查完成: 检查{}台设备", checkedCount);

        } catch (Exception e) {
            log.error("定时告警检查失败", e);
        }
    }

    /**
     * 检查单个设备的告警
     */
    private void checkDeviceAlerts(Device device) {
        String deviceId = device.getDeviceId();

        try {
            // 从Redis获取最新CPM数据
            List<RedisCacheService.RealtimeDataPoint> cpmData = redisCacheService.getRealtimeCpmData(deviceId);
            List<RedisCacheService.RealtimeDataPoint> batteryData = redisCacheService.getRealtimeBatteryData(deviceId);

            if (!cpmData.isEmpty() || !batteryData.isEmpty()) {
                // 获取最新数据
                Double latestCpm = !cpmData.isEmpty() ? cpmData.get(0).getValue() : null;
                Integer latestBattery = !batteryData.isEmpty() ? batteryData.get(0).getValue().intValue() : null;

                // 触发告警检测
                alertService.checkAlerts(deviceId, latestCpm, latestBattery, LocalDateTime.now());

                // 记录检查结果
                if (log.isDebugEnabled()) {
                    log.debug("设备告警检查完成: 设备={}, CPM={}, 电池={}V",
                            deviceId, latestCpm, latestBattery != null ? latestBattery / 1000.0 : null);
                }
            }

        } catch (Exception e) {
            log.error("检查设备告警失败: 设备={}", deviceId, e);
        }
    }

    /**
     * 获取用户有权限的设备列表
     */
    private List<Device> getUserAccessibleDevices() {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            if (SecurityUtils.isPlatformAdmin()) {
                // 平台管理员可以看到所有设备
                return deviceRepository.findAllActive(
                    org.springframework.data.domain.PageRequest.of(0, 10000)
                ).getContent();
            } else if (userEnterpriseId != null) {
                // 企业用户只能看到自己企业的设备
                return deviceRepository.findByEnterpriseId(userEnterpriseId);
            } else {
                // 企业用户只能查看自己企业的设备
                return deviceRepository.findByEnterpriseId(userEnterpriseId);
            }
        } catch (Exception e) {
            log.warn("获取用户权限设备列表失败，返回空列表", e);
            return new ArrayList<>();
        }
    }

    /**
     * 清理过期告警记录
     * 每天凌晨2点执行，清理30天前的已解决告警记录
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanExpiredAlerts() {
        try {
            log.info("开始清理过期告警记录");

            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            int deletedCount = alertRecordRepository.deleteByTriggeredAtBefore(thirtyDaysAgo);

            log.info("过期告警记录清理完成: 删除{}条记录", deletedCount);

        } catch (Exception e) {
            log.error("清理过期告警记录失败", e);
        }
    }

    /**
     * 告警统计报告
     * 每天凌晨3点生成告警统计报告
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void generateAlertReport() {
        try {
            log.info("开始生成告警统计报告");

            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime startOfYesterday = yesterday.toLocalDate().atStartOfDay();
            LocalDateTime endOfYesterday = yesterday.toLocalDate().atTime(23, 59, 59);

            // 查询昨天的告警数据
            List<Object[]> alertStats = alertRecordRepository.countAlertsBySeverityBetween(startOfYesterday, endOfYesterday);

            if (!alertStats.isEmpty()) {
                log.info("昨日告警统计报告:");
                for (Object[] stat : alertStats) {
                    log.info("  - {}级别: {}条", stat[0], stat[1]);
                }
            }

            log.info("告警统计报告生成完成");

        } catch (Exception e) {
            log.error("生成告警统计报告失败", e);
        }
    }

    /**
     * 系统健康检查
     * 每10分钟检查告警系统运行状态
     */
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public void systemHealthCheck() {
        try {
            // 检查告警数量是否异常
            long activeAlertCount = alertService.getActiveAlerts().size();

            if (activeAlertCount > 100) {
                log.warn("系统告警: 当前活跃告警数量过多({}条)，请及时处理", activeAlertCount);
            }

            // 检查Redis连接状态
            boolean redisAvailable = redisCacheService.isRedisAvailable();
            if (!redisAvailable) {
                log.error("系统告警: Redis连接异常，可能影响实时数据查询");
            }

            log.debug("告警系统健康检查完成: 活跃告警={}, Redis状态={}",
                        activeAlertCount, redisAvailable ? "正常" : "异常");

        } catch (Exception e) {
            log.error("告警系统健康检查失败", e);
        }
    }
}