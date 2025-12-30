package com.cdutetc.ems.scheduler;

import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.repository.EnvironmentDeviceDataRepository;
import com.cdutetc.ems.repository.RadiationDeviceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 监测数据批量刷新定时任务
 *
 * 功能:
 * 1. 每1分钟从Redis队列批量取出监测数据
 * 2. 批量写入MySQL(最多1000条/次)
 * 3. 降低MySQL写入频率99.5%(3.3次/秒 → 1次/分钟)
 *
 * 数据流转:
 * MQTT消息 → Redis队列 → 定时任务(每1分钟) → MySQL批量写入
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringDataFlushScheduler {

    private final RadiationDeviceDataRepository radiationRepository;
    private final EnvironmentDeviceDataRepository environmentRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    // Redis队列前缀
    private static final String RADIATION_QUEUE = "buffer:queue:radiation";
    private static final String ENV_QUEUE = "buffer:queue:environment";

    // 批量写入最大数量
    private static final int MAX_BATCH_SIZE = 1000;

    /**
     * 定时批量写入监测数据到MySQL
     *
     * 执行频率: 每1分钟执行一次
     * 执行逻辑:
     * 1. 从Redis队列批量取出数据(最多1000条)
     * 2. 使用saveAll批量写入MySQL
     * 3. 记录写入日志
     */
    @Scheduled(fixedRate = 60000)  // 1分钟 = 60000毫秒
    public void flushMonitoringDataToMySQL() {
        try {
            long startRad = flushRadiationData();
            long startEnv = flushEnvironmentData();

            // 如果两个队列都有数据,记录汇总日志
            if (startRad > 0 || startEnv > 0) {
                log.info("批量写入监测数据完成 - 辐射: {}条, 环境: {}条", startRad, startEnv);
            }
        } catch (Exception e) {
            log.error("批量写入监测数据失败", e);
        }
    }

    /**
     * 批量写入辐射设备数据
     *
     * @return 写入的数据条数
     */
    private long flushRadiationData() {
        String queueKey = RADIATION_QUEUE;
        Long size = redisTemplate.opsForList().size(queueKey);

        if (size == null || size == 0) {
            return 0;
        }

        List<RadiationDeviceData> dataList = new ArrayList<>();

        // 批量取出数据(最多1000条)
        int batchSize = Math.min(size.intValue(), MAX_BATCH_SIZE);
        for (int i = 0; i < batchSize; i++) {
            RadiationDeviceData data =
                (RadiationDeviceData) redisTemplate.opsForList().leftPop(queueKey);
            if (data != null) {
                dataList.add(data);
            }
        }

        if (!dataList.isEmpty()) {
            // 批量写入MySQL
            long startTime = System.currentTimeMillis();
            radiationRepository.saveAll(dataList);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("批量写入辐射数据: count={}, duration={}ms", dataList.size(), duration);
        }

        return dataList.size();
    }

    /**
     * 批量写入环境设备数据
     *
     * @return 写入的数据条数
     */
    private long flushEnvironmentData() {
        String queueKey = ENV_QUEUE;
        Long size = redisTemplate.opsForList().size(queueKey);

        if (size == null || size == 0) {
            return 0;
        }

        List<EnvironmentDeviceData> dataList = new ArrayList<>();

        // 批量取出数据(最多1000条)
        int batchSize = Math.min(size.intValue(), MAX_BATCH_SIZE);
        for (int i = 0; i < batchSize; i++) {
            EnvironmentDeviceData data =
                (EnvironmentDeviceData) redisTemplate.opsForList().leftPop(queueKey);
            if (data != null) {
                dataList.add(data);
            }
        }

        if (!dataList.isEmpty()) {
            // 批量写入MySQL
            long startTime = System.currentTimeMillis();
            environmentRepository.saveAll(dataList);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("批量写入环境数据: count={}, duration={}ms", dataList.size(), duration);
        }

        return dataList.size();
    }
}
