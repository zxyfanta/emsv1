package com.ems.service;

import com.ems.entity.DeviceStatusRecord;
import com.ems.repository.DeviceStatusRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量存储服务
 * 解决高并发MQTT数据写入MySQL的性能问题
 * 提供安全的数据窗口控制机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchStorageService {

    private final DeviceStatusRecordRepository deviceStatusRecordRepository;

    // 使用线程安全的队列存储待处理数据
    private final ConcurrentLinkedQueue<DeviceStatusRecord> batchQueue = new ConcurrentLinkedQueue<>();

    // 批处理配置
    private static final int MAX_BATCH_SIZE = 1000;      // 最大批次大小
    private static final int MAX_WAIT_SECONDS = 10;    // 最大等待时间
    private static final int MAX_QUEUE_SIZE = 10000;    // 最大队列大小

    // 统计信息
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalErrors = new AtomicInteger(0);
    private final AtomicInteger lastBatchSize = new AtomicInteger(0);

    /**
     * 添加数据到批处理队列
     * 提供队列大小限制，防止内存溢出
     */
    public boolean addToBatch(DeviceStatusRecord record) {
        if (batchQueue.size() >= MAX_QUEUE_SIZE) {
            log.warn("⚠️ 批处理队列已满，丢弃数据: 队列大小={}, 设备={}",
                    batchQueue.size(), record.getDevice().getDeviceId());
            return false;
        }

        boolean added = batchQueue.offer(record);
        if (added) {
            log.debug("📝 数据已加入批处理队列: 设备={}, 队列大小={}",
                     record.getDevice().getDeviceId(), batchQueue.size());
        }
        return added;
    }

    /**
     * 定时批处理任务
     * 每10秒执行一次，确保数据及时处理
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    @Transactional
    public void processBatch() {
        if (batchQueue.isEmpty()) {
            log.debug("📋 批处理队列为空，跳过处理");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        List<DeviceStatusRecord> batch = new ArrayList<>();

        try {
            // 读取批次数据
            int count = 0;
            while (count < MAX_BATCH_SIZE && !batchQueue.isEmpty()) {
                DeviceStatusRecord record = batchQueue.poll();
                if (record != null) {
                    batch.add(record);
                    count++;
                }
            }

            if (!batch.isEmpty()) {
                // 执行批量保存
                List<DeviceStatusRecord> savedRecords = deviceStatusRecordRepository.saveAll(batch);

                // 更新统计
                totalProcessed.addAndGet(batch.size());
                lastBatchSize.set(batch.size());

                long executionTime = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

                log.info("✅ 批量存储完成: 数量={}, 耗时={}ms, 队列剩余={}",
                        batch.size(), executionTime, batchQueue.size());

                // 检查数据处理健康状态
                checkProcessingHealth();
            }

        } catch (Exception e) {
            totalErrors.incrementAndGet();
            log.error("❌ 批量存储失败: 队列大小={}, 错误={}", batch.size(), e.getMessage(), e);

            // 处理失败时，将数据重新放回队列
            for (DeviceStatusRecord record : batch) {
                if (batchQueue.size() < MAX_QUEUE_SIZE) {
                    batchQueue.offer(record);
                }
            }
        }
    }

    /**
     * 紧急批处理
     * 当队列积压过多时触发
     */
    @Scheduled(fixedDelay = 2000) // 每2秒检查一次
    public void emergencyBatchProcess() {
        if (batchQueue.size() > MAX_QUEUE_SIZE * 0.8) { // 队列使用率超过80%
            log.warn("🚨 队列积压严重，触发紧急处理: 队列大小={}", batchQueue.size());
            processBatch();
        }
    }

    /**
     * 强制立即处理所有待处理数据
     * 用于系统关闭前的数据保全
     */
    @Transactional
    public int processAllRemaining() {
        if (batchQueue.isEmpty()) {
            return 0;
        }

        log.info("🔄 强制处理所有剩余数据: 队列大小={}", batchQueue.size());

        List<DeviceStatusRecord> allRecords = new ArrayList<>();
        while (!batchQueue.isEmpty()) {
            DeviceStatusRecord record = batchQueue.poll();
            if (record != null) {
                allRecords.add(record);
            }
        }

        try {
            List<DeviceStatusRecord> savedRecords = deviceStatusRecordRepository.saveAll(allRecords);
            log.info("✅ 强制处理完成: 数量={}", savedRecords.size());
            return savedRecords.size();
        } catch (Exception e) {
            log.error("❌ 强制处理失败: 数量={}", allRecords.size(), e);
            return 0;
        }
    }

    /**
     * 检查数据处理健康状态
     */
    private void checkProcessingHealth() {
        int queueSize = batchQueue.size();
        int batchSize = lastBatchSize.get();
        int total = totalProcessed.get();
        int errors = totalErrors.get();

        // 队列积压预警
        if (queueSize > MAX_QUEUE_SIZE * 0.7) {
            log.warn("⚠️ 队列积压预警: 大小={}, 阈值={}", queueSize, (int)(MAX_QUEUE_SIZE * 0.7));
        }

        // 错误率预警
        if (total > 0) {
            double errorRate = (double) errors / total * 100;
            if (errorRate > 5.0) { // 错误率超过5%
                log.warn("⚠️ 错误率过高: {:.2f}%, 成功={}, 失败={}", errorRate, total - errors, errors);
            }
        }
    }

    /**
     * 获取批处理统计信息
     */
    public BatchProcessingStats getStats() {
        return new BatchProcessingStats(
            batchQueue.size(),
            totalProcessed.get(),
            totalErrors.get(),
            lastBatchSize.get(),
            calculateErrorRate()
        );
    }

    /**
     * 计算错误率
     */
    private double calculateErrorRate() {
        int total = totalProcessed.get();
        return total > 0 ? (double) totalErrors.get() / total * 100 : 0.0;
    }

    /**
     * 批处理统计信息
     */
    public static class BatchProcessingStats {
        private final int queueSize;
        private final int totalProcessed;
        private final int totalErrors;
        private final int lastBatchSize;
        private final double errorRate;

        public BatchProcessingStats(int queueSize, int totalProcessed, int totalErrors,
                                   int lastBatchSize, double errorRate) {
            this.queueSize = queueSize;
            this.totalProcessed = totalProcessed;
            this.totalErrors = totalErrors;
            this.lastBatchSize = lastBatchSize;
            this.errorRate = errorRate;
        }

        public int getQueueSize() { return queueSize; }
        public int getTotalProcessed() { return totalProcessed; }
        public int getTotalErrors() { return totalErrors; }
        public int getLastBatchSize() { return lastBatchSize; }
        public double getErrorRate() { return errorRate; }

        @Override
        public String toString() {
            return String.format("BatchStats{queue=%d, processed=%d, errors=%d, lastBatch=%d, errorRate=%.2f%%}",
                    queueSize, totalProcessed, totalErrors, lastBatchSize, errorRate);
        }
    }
}