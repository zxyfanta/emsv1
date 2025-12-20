package com.ems.repository;

import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceType;
import com.ems.entity.enterprise.Enterprise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeviceRepository 集成测试
 *
 * @author EMS Team
 */
@DataJpaTest
@ActiveProfiles("test")
class DeviceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeviceRepository deviceRepository;

    private Enterprise testEnterprise;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        // 创建测试企业
        testEnterprise = Enterprise.builder()
                .name("测试企业")
                .build();
        entityManager.persist(testEnterprise);

        // 创建设备类型
        DeviceType deviceType = DeviceType.builder()
                .name("辐射监测仪")
                .code("RADIATION")
                .mqttTopicPattern("ems/radiation/+/data")
                .dataTable("radiation_device_status")
                .build();
        entityManager.persist(deviceType);

        // 创建测试设备
        testDevice = Device.builder()
                .deviceId("TEST-001")
                .deviceName("测试设备")
                .deviceType(Device.DeviceType.RADIATION)
                .status(Device.DeviceStatus.ONLINE)
                .enterprise(testEnterprise)
                .lastOnlineAt(LocalDateTime.now())
                .deleted(false)
                .build();
        entityManager.persist(testDevice);

        entityManager.flush();
    }

    @Test
    void testFindByDeviceId_Success() {
        // When
        Optional<Device> result = deviceRepository.findByDeviceId("TEST-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TEST-001", result.get().getDeviceId());
        assertEquals("测试设备", result.get().getDeviceName());
    }

    @Test
    void testFindByDeviceId_NotFound() {
        // When
        Optional<Device> result = deviceRepository.findByDeviceId("NON-EXISTENT");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEnterpriseIdAndDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Device> result = deviceRepository.findByEnterpriseIdAndDeletedFalse(
                testEnterprise.getId(), pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("TEST-001", result.getContent().get(0).getDeviceId());
    }

    @Test
    void testFindByEnterpriseId() {
        // When
        List<Device> result = deviceRepository.findByEnterpriseId(testEnterprise.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals("TEST-001", result.get(0).getDeviceId());
    }

    @Test
    void testFindByStatusAndDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Device> result = deviceRepository.findByStatusAndDeletedFalse(
                Device.DeviceStatus.ONLINE, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(Device.DeviceStatus.ONLINE, result.getContent().get(0).getStatus());
    }

    @Test
    void testFindByDeviceTypeAndDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Device> result = deviceRepository.findByDeviceTypeAndDeletedFalse(
                Device.DeviceType.RADIATION, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(Device.DeviceType.RADIATION, result.getContent().get(0).getDeviceType());
    }

    @Test
    void testFindByDeviceNameContainingIgnoreCaseAndDeletedFalse() {
        // When
        List<Device> result = deviceRepository.findByDeviceNameContainingIgnoreCaseAndDeletedFalse("测试");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getDeviceName().contains("测试"));
    }

    @Test
    void testCountByEnterpriseIdAndStatus() {
        // When
        Long result = deviceRepository.countByEnterpriseIdAndStatus(
                testEnterprise.getId(), Device.DeviceStatus.ONLINE);

        // Then
        assertEquals(1L, result);
    }

    @Test
    void testExistsByDeviceId() {
        // When
        boolean result = deviceRepository.existsByDeviceId("TEST-001");

        // Then
        assertTrue(result);
    }

    @Test
    void testExistsByDeviceId_NotFound() {
        // When
        boolean result = deviceRepository.existsByDeviceId("NON-EXISTENT");

        // Then
        assertFalse(result);
    }

    @Test
    void testFindByEnterpriseIdAndDeviceTypeAndDeletedFalse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Device> result = deviceRepository.findByEnterpriseIdAndDeviceTypeAndDeletedFalse(
                testEnterprise.getId(), Device.DeviceType.RADIATION, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(Device.DeviceType.RADIATION, result.getContent().get(0).getDeviceType());
    }
}