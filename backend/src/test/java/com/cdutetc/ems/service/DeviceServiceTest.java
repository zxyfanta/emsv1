package com.cdutetc.ems.service;

import com.cdutetc.ems.BaseIntegrationTest;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备服务单元测试
 */
class DeviceServiceTest extends BaseIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private Company testCompany;

    @BeforeEach
    void setUpTestData() {
        // 清理现有数据
        deviceRepository.deleteAll();
        companyRepository.deleteAll();

        // 创建测试企业
        testCompany = TestDataBuilder.buildTestCompany();
        testCompany.setStatus(CompanyStatus.ACTIVE);
        testCompany = companyRepository.save(testCompany);
    }

    @Test
    @DisplayName("创建设备 - 成功")
    void testCreateDevice_Success() {
        // 准备测试数据
        Device device = TestDataBuilder.buildTestRadiationDevice(null);
        device.setCompany(null); // 服务层会设置企业关联

        // 执行测试
        Device createdDevice = deviceService.createDevice(device, testCompany.getId());

        // 验证结果
        assertNotNull(createdDevice.getId());
        assertEquals(device.getDeviceCode(), createdDevice.getDeviceCode());
        assertEquals(device.getDeviceName(), createdDevice.getDeviceName());
        assertEquals(device.getDeviceType(), createdDevice.getDeviceType());
        assertEquals(DeviceStatus.OFFLINE, createdDevice.getStatus()); // 默认状态
        assertEquals(testCompany.getId(), createdDevice.getCompany().getId());
        assertNotNull(createdDevice.getCreatedAt());
    }

    @Test
    @DisplayName("创建设备 - 设备编码重复")
    void testCreateDevice_DuplicateDeviceCode() {
        // 创建第一个设备
        Device device1 = TestDataBuilder.buildTestRadiationDevice(null);
        device1.setCompany(null);
        deviceService.createDevice(device1, testCompany.getId());

        // 尝试创建相同设备编码的设备
        Device device2 = TestDataBuilder.buildTestRadiationDevice(null);
        device2.setDeviceCode(device1.getDeviceCode());
        device2.setCompany(null);

        // 验证抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            deviceService.createDevice(device2, testCompany.getId());
        });
    }

    @Test
    @DisplayName("获取设备详情 - 成功")
    void testGetDevice_Success() {
        // 创建测试设备
        Device device = TestDataBuilder.buildTestRadiationDevice(null);
        device.setCompany(null);
        Device createdDevice = deviceService.createDevice(device, testCompany.getId());

        // 执行测试
        Device foundDevice = deviceService.getDevice(createdDevice.getId(), testCompany.getId());

        // 验证结果
        assertNotNull(foundDevice);
        assertEquals(createdDevice.getId(), foundDevice.getId());
        assertEquals(createdDevice.getDeviceCode(), foundDevice.getDeviceCode());
    }

    @Test
    @DisplayName("获取设备详情 - 设备不存在")
    void testGetDevice_DeviceNotFound() {
        // 尝试获取不存在的设备
        assertThrows(IllegalArgumentException.class, () -> {
            deviceService.getDevice(999L, testCompany.getId());
        });
    }

    @Test
    @DisplayName("更新设备信息 - 成功")
    void testUpdateDevice_Success() {
        // 创建测试设备
        Device device = TestDataBuilder.buildTestRadiationDevice(null);
        device.setCompany(null);
        Device createdDevice = deviceService.createDevice(device, testCompany.getId());

        // 准备更新数据
        Device updateData = new Device();
        updateData.setDeviceName("更新后的设备名称");
        updateData.setDescription("更新后的设备描述");
        updateData.setLocation("更新后的位置");

        // 执行更新
        Device updatedDevice = deviceService.updateDevice(createdDevice.getId(), updateData, testCompany.getId());

        // 验证结果
        assertEquals("更新后的设备名称", updatedDevice.getDeviceName());
        assertEquals("更新后的设备描述", updatedDevice.getDescription());
        assertEquals("更新后的位置", updatedDevice.getLocation());
        assertEquals(createdDevice.getDeviceCode(), updatedDevice.getDeviceCode()); // 未更新的字段保持不变
    }

    @Test
    @DisplayName("删除设备 - 成功")
    void testDeleteDevice_Success() {
        // 创建测试设备
        Device device = TestDataBuilder.buildTestRadiationDevice(null);
        device.setCompany(null);
        Device createdDevice = deviceService.createDevice(device, testCompany.getId());

        // 验证设备存在
        assertTrue(deviceRepository.existsById(createdDevice.getId()));

        // 执行删除
        deviceService.deleteDevice(createdDevice.getId(), testCompany.getId());

        // 验证设备已删除
        assertFalse(deviceRepository.existsById(createdDevice.getId()));
    }

    @Test
    @DisplayName("获取企业设备列表 - 成功")
    void testGetDevices_Success() {
        // 创建多个测试设备
        Device device1 = TestDataBuilder.buildTestRadiationDevice(null);
        device1.setCompany(null);
        deviceService.createDevice(device1, testCompany.getId());

        Device device2 = TestDataBuilder.buildTestEnvironmentDevice(null);
        device2.setCompany(null);
        deviceService.createDevice(device2, testCompany.getId());

        // 分页查询
        Pageable pageable = PageRequest.of(0, 10);
        Page<Device> devices = deviceService.getDevices(testCompany.getId(), pageable);

        // 验证结果
        assertEquals(2, devices.getTotalElements());
        assertEquals(2, devices.getContent().size());
    }

    @Test
    @DisplayName("按设备类型获取设备列表 - 成功")
    void testGetDevicesByType_Success() {
        // 创建不同类型的设备
        Device radiationDevice = TestDataBuilder.buildTestRadiationDevice(null);
        radiationDevice.setCompany(null);
        deviceService.createDevice(radiationDevice, testCompany.getId());

        Device environmentDevice = TestDataBuilder.buildTestEnvironmentDevice(null);
        environmentDevice.setCompany(null);
        deviceService.createDevice(environmentDevice, testCompany.getId());

        // 查询辐射设备
        Pageable pageable = PageRequest.of(0, 10);
        Page<Device> radiationDevices = deviceService.getDevicesByType(
                testCompany.getId(), DeviceType.RADIATION_MONITOR, pageable);

        // 验证结果
        assertEquals(1, radiationDevices.getTotalElements());
        assertEquals(DeviceType.RADIATION_MONITOR, radiationDevices.getContent().get(0).getDeviceType());
    }

    @Test
    @DisplayName("根据设备编码获取设备 - 成功")
    void testFindByDeviceCode_Success() {
        // 创建测试设备
        Device device = TestDataBuilder.buildTestRadiationDevice(null);
        device.setCompany(null);
        Device createdDevice = deviceService.createDevice(device, testCompany.getId());

        // 执行查询
        Device foundDevice = deviceService.findByDeviceCode(createdDevice.getDeviceCode());

        // 验证结果
        assertNotNull(foundDevice);
        assertEquals(createdDevice.getId(), foundDevice.getId());
        assertEquals(createdDevice.getDeviceCode(), foundDevice.getDeviceCode());
    }

    @Test
    @DisplayName("根据设备编码获取设备 - 不存在")
    void testFindByDeviceCode_NotFound() {
        // 执行查询
        Device foundDevice = deviceService.findByDeviceCode("NONEXISTENT-DEVICE");

        // 验证结果
        assertNull(foundDevice);
    }

    @Test
    @DisplayName("获取设备统计信息 - 成功")
    void testGetDeviceStatistics_Success() {
        // 创建不同状态的设备
        Device device1 = TestDataBuilder.buildTestRadiationDevice(null);
        device1.setCompany(null);
        deviceService.createDevice(device1, testCompany.getId());

        Device device2 = TestDataBuilder.buildTestEnvironmentDevice(null);
        device2.setCompany(null);
        device2.setStatus(DeviceStatus.ONLINE);
        deviceService.createDevice(device2, testCompany.getId());

        // 获取统计信息
        DeviceService.DeviceStatistics statistics = deviceService.getDeviceStatistics(testCompany.getId());

        // 验证结果
        assertEquals(2, statistics.getTotalDevices());
        assertEquals(1, statistics.getOnlineDevices());
        assertEquals(1, statistics.getOfflineDevices());
        assertEquals(1, statistics.getRadiationDevices());
        assertEquals(1, statistics.getEnvironmentDevices());
    }
}