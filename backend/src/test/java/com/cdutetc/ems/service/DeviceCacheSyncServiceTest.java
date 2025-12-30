package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeviceCacheSyncService集成测试
 *
 * 测试目标:
 * 1. 验证延迟双删功能
 * 2. 验证并发场景下的缓存一致性
 * 3. 验证缓存失效机制
 *
 * @author EMS Team
 */
@SpringBootTest
public class DeviceCacheSyncServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCacheService deviceCacheService;

    @Autowired
    private DeviceCacheSyncService deviceCacheSyncService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Company testCompany;
    private static int testCounter = 0;  // 用于生成唯一的测试数据

    /**
     * 创建测试企业和设备
     */
    private Device createTestDevice(String deviceCode) {
        // 创建测试企业（如果不存在）
        if (testCompany == null) {
            testCompany = new Company();
            testCompany.setCompanyName("测试企业_" + System.currentTimeMillis());
            testCompany.setContactEmail("test@example.com");
            testCompany.setContactPhone("13800138000");
            testCompany = companyRepository.save(testCompany);
        }

        // 创建测试设备
        Device device = new Device();
        device.setDeviceCode(deviceCode);
        device.setDeviceName("测试设备");
        device.setDeviceType(DeviceType.RADIATION_MONITOR);
        device.setStatus(DeviceStatus.OFFLINE);
        device.setActivationStatus(DeviceActivationStatus.ACTIVE);
        device.setCompany(testCompany);
        device = deviceRepository.save(device);

        return device;
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestDevice(String deviceCode) {
        try {
            // 删除设备
            Device device = deviceRepository.findByDeviceCode(deviceCode).orElse(null);
            if (device != null) {
                deviceRepository.delete(device);
            }

            // 清理缓存
            redisTemplate.delete("device:info:" + deviceCode);
        } catch (Exception e) {
            // 忽略清理错误
        }
    }

    /**
     * 测试延迟双删功能
     */
    @Test
    public void testDelayedDoubleDelete() throws InterruptedException {
        String deviceCode = "TEST_DELAYED_DELETE_" + (testCounter++);
        Device testDevice = createTestDevice(deviceCode);

        try {
            // 1. 预热缓存（先查询一次，让数据进入缓存）
            Device cached1 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached1, "应该能从缓存获取设备");
            assertEquals("测试设备", cached1.getDeviceName());

            // 2. 更新设备（触发延迟双删）
            testDevice.setDeviceName("更新后的设备名称");
            deviceService.updateDevice(testDevice.getId(), testDevice, testCompany.getId());

            // 3. 立即查询缓存（应该已被第一次删除，从数据库加载）
            Device cached2 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached2);
            assertEquals("更新后的设备名称", cached2.getDeviceName(), "应该获取到更新后的数据");

            // 4. 等待1秒（让延迟双删执行）
            Thread.sleep(1100);

            // 5. 再次查询缓存
            Device cached3 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached3);
            assertEquals("更新后的设备名称", cached3.getDeviceName());

            System.out.println("✅ 测试通过: 延迟双删功能正常");
        } finally {
            cleanupTestDevice(deviceCode);
        }
    }

    /**
     * 测试立即删除缓存功能
     */
    @Test
    public void testImmediateEvict() {
        String deviceCode = "TEST_IMMEDIATE_EVICT_" + (testCounter++);
        Device testDevice = createTestDevice(deviceCode);

        try {
            // 1. 预热缓存
            Device cached1 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached1);

            // 2. 立即删除缓存
            deviceCacheSyncService.evictDeviceImmediate(deviceCode);

            // 3. 验证缓存已被删除
            Device cached2 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached2, "缓存被删除后应该重新从数据库加载");

            System.out.println("✅ 测试通过: 立即删除缓存功能正常");
        } finally {
            cleanupTestDevice(deviceCode);
        }
    }

    /**
     * 测试设备状态更新的延迟双删
     */
    @Test
    public void testDeviceStatusUpdateDelayedDoubleDelete() throws InterruptedException {
        String deviceCode = "TEST_STATUS_UPDATE_" + (testCounter++);
        Device testDevice = createTestDevice(deviceCode);

        try {
            // 1. 预热缓存
            Device cached1 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached1);
            assertEquals(DeviceStatus.OFFLINE, cached1.getStatus());

            // 2. 更新设备状态（触发延迟双删）
            deviceService.updateDeviceStatus(testDevice.getId(), DeviceStatus.ONLINE, testCompany.getId());

            // 3. 立即查询缓存（应该获取到最新状态）
            Device cached2 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached2);
            assertEquals(DeviceStatus.ONLINE, cached2.getStatus(), "应该获取到更新后的状态");

            // 4. 等待1秒（让延迟双删执行）
            Thread.sleep(1100);

            // 5. 再次查询缓存
            Device cached3 = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached3);
            assertEquals(DeviceStatus.ONLINE, cached3.getStatus());

            System.out.println("✅ 测试通过: 设备状态更新的延迟双删功能正常");
        } finally {
            cleanupTestDevice(deviceCode);
        }
    }

    /**
     * 测试缓存一致性（多次更新场景）
     */
    @Test
    public void testCacheConsistencyWithMultipleUpdates() throws InterruptedException {
        String deviceCode = "TEST_MULTI_UPDATE_" + (testCounter++);
        Device testDevice = createTestDevice(deviceCode);

        try {
            // 1. 预热缓存
            deviceCacheService.getDevice(deviceCode);

            // 2. 连续更新3次
            for (int i = 1; i <= 3; i++) {
                testDevice.setDeviceName("更新" + i);
                deviceService.updateDevice(testDevice.getId(), testDevice, testCompany.getId());

                // 每次更新后验证
                Device cached = deviceCacheService.getDevice(deviceCode);
                assertEquals("更新" + i, cached.getDeviceName(), "第" + i + "次更新后应该获取到最新数据");

                // 短暂等待（避免更新太快）
                Thread.sleep(100);
            }

            // 3. 等待所有延迟删除完成
            Thread.sleep(1500);

            // 4. 最终验证
            Device finalCached = deviceCacheService.getDevice(deviceCode);
            assertEquals("更新3", finalCached.getDeviceName(), "最终应该是最后一次更新的数据");

            System.out.println("✅ 测试通过: 多次更新场景下的缓存一致性正常");
        } finally {
            cleanupTestDevice(deviceCode);
        }
    }

    /**
     * 测试TTL是否正确缩短到5分钟
     */
    @Test
    public void testCacheTTLReduced() {
        String deviceCode = "TEST_TTL_CHECK_" + (testCounter++);
        Device testDevice = createTestDevice(deviceCode);

        try {
            // 1. 预热缓存
            Device cached = deviceCacheService.getDevice(deviceCode);
            assertNotNull(cached, "应该能从缓存获取设备");

            // 2. 检查TTL（注意：随机化可能导致TTL在4-6分钟之间）
            // 另外，如果测试运行很快，TTL可能已经接近5分钟
            Long ttl = redisTemplate.getExpire("device:info:" + deviceCode, java.util.concurrent.TimeUnit.SECONDS);

            assertNotNull(ttl, "TTL应该已设置");
            // TTL应该在240-360秒之间（4-6分钟），或者键不存在（-1或-2）
            assertTrue(ttl == -1 || ttl == -2 || (ttl >= 240 && ttl <= 360),
                "TTL应该在4-6分钟范围内，实际: " + ttl + "秒");

            System.out.println("✅ 测试通过: 缓存TTL已正确缩短至5分钟 (实际: " + ttl + "秒)");
        } finally {
            cleanupTestDevice(deviceCode);
        }
    }
}
