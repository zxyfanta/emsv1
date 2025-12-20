package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 设备管理服务类
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;

    /**
     * 创建设备
     */
    public Device createDevice(Device device, Long companyId) {
        log.debug("Creating device: {} for company: {}", device.getDeviceCode(), companyId);

        // 检查设备编码是否已存在
        if (deviceRepository.existsByDeviceCode(device.getDeviceCode())) {
            throw new IllegalArgumentException("设备编码 " + device.getDeviceCode() + " 已存在");
        }

        // 设置企业关联
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("企业不存在"));
        device.setCompany(company);

        // 设置默认状态
        if (device.getStatus() == null) {
            device.setStatus(DeviceStatus.OFFLINE);
        }

        Device savedDevice = deviceRepository.save(device);
        log.info("Device created successfully: {} with ID: {}", savedDevice.getDeviceCode(), savedDevice.getId());

        return savedDevice;
    }

    /**
     * 获取设备详情
     */
    @Transactional(readOnly = true)
    public Device getDevice(Long id, Long companyId) {
        log.debug("Getting device with ID: {} for company: {}", id, companyId);

        return deviceRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在或不属于当前企业"));
    }

    /**
     * 更新设备信息
     */
    public Device updateDevice(Long id, Device device, Long companyId) {
        log.debug("Updating device with ID: {} for company: {}", id, companyId);

        Device existingDevice = getDevice(id, companyId);

        // 更新允许修改的字段
        if (device.getDeviceName() != null) {
            existingDevice.setDeviceName(device.getDeviceName());
        }
        if (device.getDescription() != null) {
            existingDevice.setDescription(device.getDescription());
        }
        if (device.getLocation() != null) {
            existingDevice.setLocation(device.getLocation());
        }

        Device updatedDevice = deviceRepository.save(existingDevice);
        log.info("Device updated successfully: {} with ID: {}", updatedDevice.getDeviceCode(), updatedDevice.getId());

        return updatedDevice;
    }

    /**
     * 删除设备
     */
    public void deleteDevice(Long id, Long companyId) {
        log.debug("Deleting device with ID: {} for company: {}", id, companyId);

        Device device = getDevice(id, companyId);
        deviceRepository.delete(device);

        log.info("Device deleted successfully: {} with ID: {}", device.getDeviceCode(), id);
    }

    /**
     * 获取企业设备列表
     */
    @Transactional(readOnly = true)
    public Page<Device> getDevices(Long companyId, Pageable pageable) {
        log.debug("Getting devices for company: {} with pageable: {}", companyId, pageable);

        return deviceRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * 按设备类型获取设备列表
     */
    @Transactional(readOnly = true)
    public Page<Device> getDevicesByType(Long companyId, DeviceType deviceType, Pageable pageable) {
        log.debug("Getting devices for company: {} with type: {}", companyId, deviceType);

        return deviceRepository.findByCompanyIdAndDeviceType(companyId, deviceType, pageable);
    }

    /**
     * 搜索设备
     */
    @Transactional(readOnly = true)
    public Page<Device> searchDevices(Long companyId, String keyword, Pageable pageable) {
        log.debug("Searching devices for company: {} with keyword: {}", companyId, keyword);

        return deviceRepository.searchByCompanyIdAndKeyword(companyId, keyword, pageable);
    }

    /**
     * 更新设备状态
     */
    public Device updateDeviceStatus(Long id, DeviceStatus status, Long companyId) {
        log.debug("Updating device status for ID: {} to {} for company: {}", id, status, companyId);

        Device device = getDevice(id, companyId);
        device.setStatus(status);

        // 如果设备上线，更新最后在线时间
        if (status == DeviceStatus.ONLINE) {
            device.setLastOnlineAt(java.time.LocalDateTime.now());
        }

        Device updatedDevice = deviceRepository.save(device);
        log.info("Device status updated successfully: {} to {}", device.getDeviceCode(), status);

        return updatedDevice;
    }

    /**
     * 根据设备编码获取设备（不限企业）
     */
    @Transactional(readOnly = true)
    public Device findByDeviceCode(String deviceCode) {
        log.debug("Getting device by code: {}", deviceCode);

        return deviceRepository.findByDeviceCode(deviceCode).orElse(null);
    }

    /**
     * 根据设备编码获取设备
     */
    @Transactional(readOnly = true)
    public Optional<Device> getDeviceByCode(String deviceCode, Long companyId) {
        log.debug("Getting device by code: {} for company: {}", deviceCode, companyId);

        return deviceRepository.findByDeviceCodeAndCompanyId(deviceCode, companyId);
    }

    /**
     * 验证设备访问权限
     */
    @Transactional(readOnly = true)
    public void validateDeviceAccess(String deviceCode, Long companyId) {
        log.debug("Validating device access: {} for company: {}", deviceCode, companyId);

        if (!deviceRepository.existsByDeviceCodeAndCompanyId(deviceCode, companyId)) {
            throw new IllegalArgumentException("设备 " + deviceCode + " 不存在或不属于当前企业");
        }
    }

    /**
     * 获取设备统计信息
     */
    @Transactional(readOnly = true)
    public DeviceStatistics getDeviceStatistics(Long companyId) {
        log.debug("Getting device statistics for company: {}", companyId);

        long totalDevices = deviceRepository.countByCompanyId(companyId);
        long onlineDevices = deviceRepository.countByCompanyIdAndStatus(companyId, DeviceStatus.ONLINE);
        long offlineDevices = deviceRepository.countByCompanyIdAndStatus(companyId, DeviceStatus.OFFLINE);
        long faultDevices = deviceRepository.countByCompanyIdAndStatus(companyId, DeviceStatus.FAULT);
        long maintenanceDevices = deviceRepository.countByCompanyIdAndStatus(companyId, DeviceStatus.MAINTENANCE);

        long radiationDevices = deviceRepository.countByCompanyIdAndDeviceType(companyId, DeviceType.RADIATION_MONITOR);
        long environmentDevices = deviceRepository.countByCompanyIdAndDeviceType(companyId, DeviceType.ENVIRONMENT_STATION);

        return DeviceStatistics.builder()
                .totalDevices(totalDevices)
                .onlineDevices(onlineDevices)
                .offlineDevices(offlineDevices)
                .faultDevices(faultDevices)
                .maintenanceDevices(maintenanceDevices)
                .radiationDevices(radiationDevices)
                .environmentDevices(environmentDevices)
                .build();
    }

    /**
     * 设备统计信息DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DeviceStatistics {
        private long totalDevices;
        private long onlineDevices;
        private long offlineDevices;
        private long faultDevices;
        private long maintenanceDevices;
        private long radiationDevices;
        private long environmentDevices;
    }
}