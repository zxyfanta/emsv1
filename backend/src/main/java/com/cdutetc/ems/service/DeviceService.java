package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceActivationCodeRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final DeviceActivationCodeRepository activationCodeRepository;
    private final DeviceReportConfigCacheService cacheService;
    private final DeviceCacheService deviceCacheService;
    private final DeviceCacheSyncService deviceCacheSyncService;

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

        // 更新可视化坐标
        if (device.getPositionX() != null) {
            existingDevice.setPositionX(device.getPositionX());
            log.debug("Updated positionX to: {}", device.getPositionX());
        }
        if (device.getPositionY() != null) {
            existingDevice.setPositionY(device.getPositionY());
            log.debug("Updated positionY to: {}", device.getPositionY());
        }

        // 更新辐射设备上报专用字段
        if (device.getNuclide() != null) {
            existingDevice.setNuclide(device.getNuclide());
        }
        if (device.getInspectionMachineNumber() != null) {
            existingDevice.setInspectionMachineNumber(device.getInspectionMachineNumber());
        }
        if (device.getSourceNumber() != null) {
            existingDevice.setSourceNumber(device.getSourceNumber());
        }
        if (device.getSourceType() != null) {
            existingDevice.setSourceType(device.getSourceType());
        }
        if (device.getOriginalActivity() != null) {
            existingDevice.setOriginalActivity(device.getOriginalActivity());
        }
        if (device.getCurrentActivity() != null) {
            existingDevice.setCurrentActivity(device.getCurrentActivity());
        }
        if (device.getSourceProductionDate() != null) {
            existingDevice.setSourceProductionDate(device.getSourceProductionDate());
        }

        // 更新上报配置
        if (device.getDataReportEnabled() != null) {
            existingDevice.setDataReportEnabled(device.getDataReportEnabled());
        }
        if (device.getReportProtocol() != null) {
            existingDevice.setReportProtocol(device.getReportProtocol());
        }

        Device updatedDevice = deviceRepository.save(existingDevice);

        // 清除设备信息缓存（立即删除 + 延迟双删）
        deviceCacheService.evictDevice(updatedDevice.getDeviceCode());
        deviceCacheSyncService.evictDeviceWithDelay(updatedDevice.getDeviceCode());  // 延迟1秒再次删除
        log.debug("已清除设备信息缓存（延迟双删）: deviceCode={}", updatedDevice.getDeviceCode());

        // 清除 Redis 缓存（只针对辐射设备）
        if (updatedDevice.getDeviceType() == DeviceType.RADIATION_MONITOR) {
            cacheService.evictReportConfig(updatedDevice.getDeviceCode());
            log.debug("已清除设备上报配置缓存: deviceCode={}", updatedDevice.getDeviceCode());
        }

        log.info("Device updated successfully: {} with ID: {}, position: ({}, {})",
            updatedDevice.getDeviceCode(), updatedDevice.getId(),
            updatedDevice.getPositionX(), updatedDevice.getPositionY());

        return updatedDevice;
    }

    /**
     * 删除设备
     */
    public void deleteDevice(Long id, Long companyId) {
        log.debug("Deleting device with ID: {} for company: {}", id, companyId);

        Device device = getDevice(id, companyId);
        String deviceCode = device.getDeviceCode();

        deviceRepository.delete(device);

        // 清除设备信息缓存
        deviceCacheService.evictDevice(deviceCode);
        log.debug("已清除设备信息缓存: deviceCode={}", deviceCode);

        log.info("Device deleted successfully: {} with ID: {}", deviceCode, id);
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
     * 按激活状态获取企业设备列表
     */
    @Transactional(readOnly = true)
    public Page<Device> getDevicesByActivationStatus(Long companyId, DeviceActivationStatus activationStatus, Pageable pageable) {
        log.debug("Getting devices for company: {} with activation status: {}", companyId, activationStatus);

        return deviceRepository.findByCompanyIdAndActivationStatus(companyId, activationStatus, pageable);
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

        // 清除设备状态缓存（立即删除 + 延迟双删）
        deviceCacheService.evictDevice(updatedDevice.getDeviceCode());
        deviceCacheSyncService.evictDeviceWithDelay(updatedDevice.getDeviceCode());  // 延迟1秒再次删除
        log.debug("已清除设备状态缓存（延迟双删）: deviceCode={}", updatedDevice.getDeviceCode());

        log.info("Device status updated successfully: {} to {}", device.getDeviceCode(), status);

        return updatedDevice;
    }

    /**
     * 根据设备编码获取设备（不限企业）
     * 使用DeviceCacheService缓存,减少数据库查询
     */
    @Transactional(readOnly = true)
    public Device findByDeviceCode(String deviceCode) {
        log.debug("Getting device by code: {}", deviceCode);

        return deviceCacheService.getDevice(deviceCode);
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
     * 根据ID获取设备（管理员专用，不检查企业权限）
     */
    @Transactional(readOnly = true)
    public Device getDeviceById(Long id) {
        log.debug("Getting device by ID: {}", id);
        return deviceRepository.findById(id).orElse(null);
    }

    /**
     * 获取设备统计信息
     * 使用聚合查询优化(7次查询→1次) + Spring Cache缓存
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "deviceStats", key = "#companyId")
    public DeviceStatistics getDeviceStatistics(Long companyId) {
        log.debug("Getting device statistics for company: {}", companyId);

        // 使用聚合查询一次性获取所有统计信息
        List<DeviceRepository.DeviceStatsProjection> stats =
            deviceRepository.getStatisticsGrouped(companyId);

        // 初始化计数器
        long totalDevices = 0;
        long onlineDevices = 0;
        long offlineDevices = 0;
        long faultDevices = 0;
        long maintenanceDevices = 0;
        long radiationDevices = 0;
        long environmentDevices = 0;
        long pendingCount = 0;
        long activatedCount = 0;

        // 解析聚合查询结果
        for (DeviceRepository.DeviceStatsProjection stat : stats) {
            long count = stat.getCount() != null ? stat.getCount() : 0;

            // 累计总数
            totalDevices += count;

            // 按状态统计
            if (DeviceStatus.ONLINE.name().equals(stat.getStatus())) {
                onlineDevices += count;
            } else if (DeviceStatus.OFFLINE.name().equals(stat.getStatus())) {
                offlineDevices += count;
            } else if (DeviceStatus.FAULT.name().equals(stat.getStatus())) {
                faultDevices += count;
            } else if (DeviceStatus.MAINTENANCE.name().equals(stat.getStatus())) {
                maintenanceDevices += count;
            }

            // 按设备类型统计
            if (DeviceType.RADIATION_MONITOR.name().equals(stat.getDeviceType())) {
                radiationDevices += count;
            } else if (DeviceType.ENVIRONMENT_STATION.name().equals(stat.getDeviceType())) {
                environmentDevices += count;
            }

            // 按激活状态统计
            if (DeviceActivationStatus.PENDING.name().equals(stat.getActivationStatus())) {
                pendingCount += count;
            } else if (DeviceActivationStatus.ACTIVE.name().equals(stat.getActivationStatus())) {
                activatedCount += count;
            }
        }

        // 激活码数量仍需单独查询
        long activationCodeCount = activationCodeRepository.countByStatus(
            com.cdutetc.ems.entity.enums.ActivationCodeStatus.UNUSED);

        return DeviceStatistics.builder()
                .totalDevices(totalDevices)
                .onlineDevices(onlineDevices)
                .offlineDevices(offlineDevices)
                .faultDevices(faultDevices)
                .maintenanceDevices(maintenanceDevices)
                .radiationDevices(radiationDevices)
                .environmentDevices(environmentDevices)
                .pendingCount(pendingCount)
                .activatedCount(activatedCount)
                .activationCodeCount(activationCodeCount)
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
        // 管理员统计字段
        private long pendingCount;
        private long activatedCount;
        private long activationCodeCount;
    }
}