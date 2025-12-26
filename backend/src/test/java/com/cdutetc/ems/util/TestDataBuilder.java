package com.cdutetc.ems.util;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.entity.enums.UserRole;
import com.cdutetc.ems.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试数据构建器
 */
public class TestDataBuilder {

    // 使用原子计数器确保设备代码唯一性
    private static final AtomicLong deviceCounter = new AtomicLong(0);
    private static final AtomicLong userCounter = new AtomicLong(0);
    private static final AtomicLong companyCounter = new AtomicLong(0);

    /**
     * 构建测试企业
     */
    public static Company buildTestCompany() {
        Company company = new Company();
        // 不设置ID，让数据库自动生成
        company.setCompanyName("测试企业-" + companyCounter.incrementAndGet());
        company.setContactEmail("test@example.com");
        company.setContactPhone("13800138000");
        company.setAddress("测试地址");
        company.setDescription("测试企业描述");
        company.setStatus(CompanyStatus.ACTIVE);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        return company;
    }

    /**
     * 构建测试用户
     */
    public static User buildTestUser(Long companyId, UserRole role) {
        User user = new User();
        // 不设置ID，让数据库自动生成
        long userId = userCounter.incrementAndGet();
        user.setUsername("testuser-" + userId);
        user.setFullName("测试用户");
        user.setEmail("testuser-" + userId + "@example.com");
        user.setPassword("$2a$10$encrypted.password.here");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        Company company = new Company();
        company.setId(companyId);
        user.setCompany(company);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * 构建测试管理员用户
     */
    public static User buildTestAdminUser(Long companyId) {
        return buildTestUser(companyId, UserRole.ADMIN);
    }

    /**
     * 构建测试普通用户
     */
    public static User buildTestNormalUser(Long companyId) {
        return buildTestUser(companyId, UserRole.USER);
    }

    /**
     * 构建测试设备
     */
    public static Device buildTestDevice(Long companyId, DeviceType deviceType) {
        Device device = new Device();
        // 不设置ID，让数据库自动生成
        long deviceId = deviceCounter.incrementAndGet();
        device.setDeviceCode("TEST-DEVICE-" + deviceId);
        device.setDeviceName("测试设备");
        device.setDeviceType(deviceType);
        device.setManufacturer("测试厂商");
        device.setModel("测试型号");
        device.setSerialNumber("TEST-SN-" + deviceId);
        device.setDescription("测试设备描述");
        device.setLocation("测试位置");
        device.setStatus(DeviceStatus.OFFLINE);
        Company company = new Company();
        company.setId(companyId);
        device.setCompany(company);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        device.setInstallDate(LocalDateTime.now());
        return device;
    }

    /**
     * 构建测试辐射监测设备
     */
    public static Device buildTestRadiationDevice(Long companyId) {
        return buildTestDevice(companyId, DeviceType.RADIATION_MONITOR);
    }

    /**
     * 构建测试环境监测站
     */
    public static Device buildTestEnvironmentDevice(Long companyId) {
        return buildTestDevice(companyId, DeviceType.ENVIRONMENT_STATION);
    }

    /**
     * 构建测试辐射设备数据请求
     */
    public static com.cdutetc.ems.dto.request.RadiationDataReceiveRequest buildRadiationDataRequest(String deviceCode) {
        com.cdutetc.ems.dto.request.RadiationDataReceiveRequest request = new com.cdutetc.ems.dto.request.RadiationDataReceiveRequest();
        request.setDeviceCode(deviceCode);
        request.setRawData("{\"test\":\"data\"}");
        request.setSrc(1);
        request.setMsgtype(1);
        request.setCpm(12.5);
        request.setBatvolt(3.7);
        request.setTime("2025-12-21 01:00:00");
        request.setTrigger(1);
        request.setMulti(1);
        request.setWay(1);
        request.setBdsLongitude("116.404");
        request.setBdsLatitude("39.915");
        request.setBdsUtc("2025-12-21 01:00:00");
        // bdsUseful字段在DTO中不存在，所以删除
        request.setLbsLongitude("116.404");
        request.setLbsLatitude("39.915");
        request.setLbsUseful(1);
        return request;
    }

    /**
     * 构建测试环境设备数据请求
     */
    public static com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest buildEnvironmentDataRequest(String deviceCode) {
        com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest request = new com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest();
        request.setDeviceCode(deviceCode);
        request.setRawData("{\"test\":\"data\"}");
        request.setSrc(1);
        request.setCpm(15.2);
        request.setTemperature(25.5);
        request.setWetness(60.8);
        request.setWindspeed(5.2);
        request.setTotal(100.0);
        request.setBattery(4.1);
        return request;
    }
}