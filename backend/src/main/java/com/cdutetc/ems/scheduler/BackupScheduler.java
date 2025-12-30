package com.cdutetc.ems.scheduler;

import com.cdutetc.ems.config.BackupProperties;
import com.cdutetc.ems.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据备份定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.ems.backup",
    name = "enabled",
    havingValue = "true"
)
public class BackupScheduler {

    private final BackupService backupService;
    private final BackupProperties backupProperties;

    /**
     * 定时执行备份任务
     * Cron表达式在配置文件中定义
     */
    @Scheduled(cron = "${app.ems.backup.scheduler.cron:0 3 1 * *}")
    public void scheduledBackup() {
        if (!backupProperties.getScheduler().isEnabled()) {
            log.debug("备份定时任务已禁用，跳过执行");
            return;
        }

        log.info("========================================");
        log.info("开始执行定时备份任务");
        log.info("触发方式: SCHEDULED");
        log.info("========================================");

        try {
            backupService.backupAll("SCHEDULED");
            log.info("定时备份任务执行完成");
        } catch (Exception e) {
            log.error("定时备份任务执行失败", e);
        }
    }
}
