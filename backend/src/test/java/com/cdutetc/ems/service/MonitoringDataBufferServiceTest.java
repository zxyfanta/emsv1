package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.entity.RadiationDeviceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MonitoringDataBufferService单元测试
 *
 * 测试目标:
 * 1. 验证辐射数据写入缓冲区
 * 2. 验证环境数据写入缓冲区
 * 3. 验证Redis实时查询功能
 * 4. 验证批量队列功能
 *
 * @author EMS Team
 */
@SpringBootTest
public class MonitoringDataBufferServiceTest {

    @Autowired
    private MonitoringDataBufferService monitoringDataBufferService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_DEVICE_CODE = "TEST_RAD_001";

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        redisTemplate.delete("monitoring:radiation:" + TEST_DEVICE_CODE);
        redisTemplate.delete("buffer:queue:radiation");
        redisTemplate.delete("monitoring:env:" + TEST_DEVICE_CODE);
        redisTemplate.delete("buffer:queue:environment");
    }

    /**
     * 测试辐射数据写入缓冲区
     */
    @Test
    public void testSaveRadiationDataToBuffer() {
        // 准备测试数据
        RadiationDeviceData data = createTestRadiationData(TEST_DEVICE_CODE, 100.0, 3.7);

        // 执行：写入缓冲区
        monitoringDataBufferService.saveRadiationDataToBuffer(data);

        // 验证1: Redis缓存中有数据
        RadiationDeviceData cached = monitoringDataBufferService.getLatestRadiationData(TEST_DEVICE_CODE);
        assertNotNull(cached, "Redis中应该有辐射数据");
        assertEquals(TEST_DEVICE_CODE, cached.getDeviceCode());
        assertEquals(100.0, cached.getCpm());
        assertEquals(3.7, cached.getBatvolt());

        // 验证2: 批量队列中有数据
        Long queueSize = redisTemplate.opsForList().size("buffer:queue:radiation");
        assertNotNull(queueSize);
        assertEquals(1, queueSize, "批量队列中应该有1条数据");

        System.out.println("✅ 测试通过: 辐射数据写入缓冲区成功");
    }

    /**
     * 测试环境数据写入缓冲区
     */
    @Test
    public void testSaveEnvironmentDataToBuffer() {
        // 准备测试数据
        EnvironmentDeviceData data = createTestEnvironmentData(TEST_DEVICE_CODE, 25.5, 11.2);

        // 执行：写入缓冲区
        monitoringDataBufferService.saveEnvironmentDataToBuffer(data);

        // 验证1: Redis缓存中有数据
        EnvironmentDeviceData cached = monitoringDataBufferService.getLatestEnvironmentData(TEST_DEVICE_CODE);
        assertNotNull(cached, "Redis中应该有环境数据");
        assertEquals(TEST_DEVICE_CODE, cached.getDeviceCode());
        assertEquals(25.5, cached.getTemperature());
        assertEquals(11.2, cached.getBattery());

        // 验证2: 批量队列中有数据
        Long queueSize = redisTemplate.opsForList().size("buffer:queue:environment");
        assertNotNull(queueSize);
        assertEquals(1, queueSize, "批量队列中应该有1条数据");

        System.out.println("✅ 测试通过: 环境数据写入缓冲区成功");
    }

    /**
     * 测试批量队列累积
     */
    @Test
    public void testBatchQueueAccumulation() {
        // 写入3条数据
        for (int i = 0; i < 3; i++) {
            RadiationDeviceData data = createTestRadiationData(TEST_DEVICE_CODE, 100.0 + i, 3.7);
            monitoringDataBufferService.saveRadiationDataToBuffer(data);
        }

        // 验证队列大小
        long queueSize = monitoringDataBufferService.getQueueSize("radiation");
        assertEquals(3, queueSize, "批量队列中应该有3条数据");

        System.out.println("✅ 测试通过: 批量队列累积成功");
    }

    /**
     * 测试Redis实时查询性能
     */
    @Test
    public void testRedisQueryPerformance() {
        // 准备测试数据
        RadiationDeviceData data = createTestRadiationData(TEST_DEVICE_CODE, 100.0, 3.7);
        monitoringDataBufferService.saveRadiationDataToBuffer(data);

        // 测试查询性能
        long start = System.nanoTime();
        RadiationDeviceData result = monitoringDataBufferService.getLatestRadiationData(TEST_DEVICE_CODE);
        long duration = (System.nanoTime() - start) / 1_000_000;  // 转换为毫秒

        assertNotNull(result);
        assertTrue(duration < 10, "Redis查询应该在10ms内完成，实际: " + duration + "ms");

        System.out.println("✅ 测试通过: Redis查询性能验证通过 (耗时: " + duration + "ms)");
    }

    /**
     * 测试缓存过期机制
     */
    @Test
    public void testCacheExpiration() throws InterruptedException {
        // 写入测试数据
        RadiationDeviceData data = createTestRadiationData(TEST_DEVICE_CODE, 100.0, 3.7);
        monitoringDataBufferService.saveRadiationDataToBuffer(data);

        // 立即查询，应该有数据
        RadiationDeviceData cached = monitoringDataBufferService.getLatestRadiationData(TEST_DEVICE_CODE);
        assertNotNull(cached, "刚写入的数据应该能查到");

        // 注意：由于TTL是10分钟，我们不能真的等待10分钟
        // 这里只是验证TTL设置是否正确
        Long ttl = redisTemplate.getExpire("monitoring:radiation:" + TEST_DEVICE_CODE, TimeUnit.MINUTES);
        assertNotNull(ttl, "TTL应该已设置");
        assertTrue(ttl > 0 && ttl <= 10, "TTL应该在(0, 10]分钟范围内，实际: " + ttl);

        System.out.println("✅ 测试通过: 缓存TTL设置正确 (剩余: " + ttl + "分钟)");
    }

    // ========== 辅助方法 ==========

    private RadiationDeviceData createTestRadiationData(String deviceCode, double cpm, double batvolt) {
        RadiationDeviceData data = new RadiationDeviceData();
        data.setDeviceCode(deviceCode);
        data.setCpm(cpm);
        data.setBatvolt(batvolt);
        data.setRawData("{\"CPM\":" + cpm + ",\"Batvolt\":" + (batvolt * 1000) + "}");
        data.setRecordTime(LocalDateTime.now());
        return data;
    }

    private EnvironmentDeviceData createTestEnvironmentData(String deviceCode, double temperature, double battery) {
        EnvironmentDeviceData data = new EnvironmentDeviceData();
        data.setDeviceCode(deviceCode);
        data.setTemperature(temperature);
        data.setBattery(battery);
        data.setRawData("{\"temperature\":" + temperature + ",\"battery\":" + battery + "}");
        data.setRecordTime(LocalDateTime.now());
        return data;
    }
}
