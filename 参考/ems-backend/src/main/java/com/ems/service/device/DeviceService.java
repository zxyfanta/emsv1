package com.ems.service.device;

import com.ems.dto.device.*;
import com.ems.entity.DeviceSensorData;
import com.ems.entity.device.Device;
import com.ems.entity.enterprise.Enterprise;
import com.ems.repository.DeviceSensorDataRepository;
import com.ems.repository.device.DeviceRepository;
import com.ems.repository.enterprise.EnterpriseRepository;
import com.ems.service.AlertService;
import com.ems.service.DeviceOnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备管理服务
 * 提供设备查询、状态管理等功能
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final DeviceSensorDataRepository deviceSensorDataRepository;
    private final AlertService alertService;
    private final DeviceOnlineStatusService deviceOnlineStatusService;

    /**
     * 根据设备ID查找设备
     */
    public Optional<Device> findByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }

    /**
     * 获取所有设备（软删除过滤）
     */
    public Page<Device> findAllDevices(Pageable pageable) {
        log.info("获取设备列表，参数: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Device> devices = deviceRepository.findAllActive(pageable);
        log.info("查询到设备数量: {}", devices.getTotalElements());
        return devices;
    }

    /**
     * 获取在线设备
     */
    public List<Device> findOnlineDevices() {
        return deviceRepository.findOnlineDevices();
    }

    /**
     * 获取离线设备
     */
    public List<Device> findOfflineDevices() {
        return deviceRepository.findOfflineDevices();
    }

    /**
     * 根据设备类型查找设备
     */
    public List<Device> findByDeviceType(String deviceType) {
        // 简化版本：设备类型已移除，返回所有设备
        return deviceRepository.findAll();
    }

    /**
     * 保存设备
     */
    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }

    /**
     * 更新设备状态
     */
    public boolean updateDeviceStatus(String deviceId, Device.DeviceStatus status) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(device -> {
                    device.setStatus(status);
                    if (status == Device.DeviceStatus.ONLINE) {
                        device.setLastOnlineAt(LocalDateTime.now());
                    }
                    deviceRepository.save(device);
                    log.info("设备状态已更新: {} -> {}", deviceId, status);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 获取设备统计数据（基于数据库字段）
     */
    public DeviceStats getDeviceStats() {
        // 使用现有的分页查询来获取准确的未删除设备数量
        long totalCount = deviceRepository.findAllActive(
            org.springframework.data.domain.PageRequest.of(0, 1)
        ).getTotalElements();

        long onlineCount = deviceRepository.findOnlineDevices().size();
        long offlineCount = deviceRepository.findOfflineDevices().size();

        return new DeviceStats(totalCount, onlineCount, offlineCount);
    }

    /**
     * 获取智能设备统计数据（基于Redis实时数据）
     */
    public DeviceOnlineStatusService.DeviceOnlineStats getSmartDeviceStats() {
        try {
            return deviceOnlineStatusService.getDeviceOnlineStats();
        } catch (Exception e) {
            log.warn("Redis状态统计失败，回退到数据库统计", e);
            // 回退到数据库统计
            DeviceStats dbStats = getDeviceStats();
            return new DeviceOnlineStatusService.DeviceOnlineStats(
                dbStats.getTotalCount(),
                dbStats.getOnlineCount(),
                dbStats.getOfflineCount(),
                0 // 数据库统计没有警告状态
            );
        }
    }

    /**
     * 智能获取设备在线状态
     * 优先使用Redis实时数据，回退到数据库字段
     */
    public Device.DeviceStatus getSmartDeviceStatus(String deviceId) {
        try {
            return deviceOnlineStatusService.getSmartDeviceStatus(deviceId);
        } catch (Exception e) {
            log.warn("智能状态获取失败，使用数据库字段: 设备={}", deviceId, e);
            return deviceRepository.findByDeviceId(deviceId)
                .map(Device::getStatus)
                .orElse(Device.DeviceStatus.OFFLINE);
        }
    }

    /**
     * 批量获取设备在线状态
     */
    public java.util.Map<String, Device.DeviceStatus> batchGetSmartDeviceStatus(java.util.List<String> deviceIds) {
        try {
            java.util.Map<String, DeviceOnlineStatusService.DeviceOnlineStatus> onlineStatuses =
                deviceOnlineStatusService.batchGetOnlineStatus(deviceIds);

            return onlineStatuses.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    java.util.Map.Entry::getKey,
                    entry -> entry.getValue().getDeviceStatus()
                ));
        } catch (Exception e) {
            log.warn("批量智能状态获取失败，使用数据库查询", e);
            return deviceIds.stream()
                .collect(java.util.stream.Collectors.toMap(
                    deviceId -> deviceId,
                    deviceId -> deviceRepository.findByDeviceId(deviceId)
                        .map(Device::getStatus)
                        .orElse(Device.DeviceStatus.OFFLINE)
                ));
        }
    }

    /**
     * 创建设备
     */
    @Transactional
    public Device createDevice(DeviceCreateRequest request, Long enterpriseId) {
        // 检查设备ID是否已存在
        if (deviceRepository.findByDeviceId(request.getDeviceId()).isPresent()) {
            throw new IllegalArgumentException("设备ID已存在: " + request.getDeviceId());
        }

        // 验证企业存在性
        Enterprise enterprise = null;
        if (enterpriseId != null) {
            enterprise = enterpriseRepository.findByIdAndDeletedFalse(enterpriseId)
                .orElseThrow(() -> new IllegalArgumentException("企业不存在: " + enterpriseId));
        } else {
            throw new IllegalArgumentException("企业ID不能为空");
        }

        // 创建设备
        Device device = new Device();
        device.setDeviceId(request.getDeviceId());
        device.setDeviceName(request.getDeviceName());
        device.setEnterprise(enterprise);
        device.setStatus(Device.DeviceStatus.OFFLINE);
        device.setDeleted(false);

        Device savedDevice = deviceRepository.save(device);
        log.info("设备基本信息创建成功: {}", savedDevice.getDeviceId());

        // 初始化默认告警规则
        try {
            alertService.initializeDefaultAlertRules(savedDevice);
            log.info("✅ 设备默认告警规则初始化完成: {}", savedDevice.getDeviceId());
        } catch (Exception e) {
            log.error("⚠️ 设备默认告警规则初始化失败，但设备创建成功: deviceId={}",
                     savedDevice.getDeviceId(), e);
            // 不影响主要流程，只记录日志
        }

        log.info("设备创建完成: {}", savedDevice.getDeviceId());
        return savedDevice;
    }

    /**
     * 更新设备
     */
    @Transactional
    public Optional<Device> updateDevice(String deviceId, DeviceUpdateRequest request, Long enterpriseId) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(device -> {
                    // 检查企业权限
                    if (enterpriseId != null && !device.getEnterpriseId().equals(enterpriseId)) {
                        throw new IllegalArgumentException("无权限修改此设备");
                    }

                    // 更新设备信息
                    if (request.getDeviceName() != null) {
                        device.setDeviceName(request.getDeviceName());
                    }

                    if (request.getStatus() != null) {
                        try {
                            Device.DeviceStatus newStatus = Device.DeviceStatus.valueOf(request.getStatus());
                            device.setStatus(newStatus);
                            if (newStatus == Device.DeviceStatus.ONLINE) {
                                device.setLastOnlineAt(LocalDateTime.now());
                            }
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("无效的设备状态: " + request.getStatus());
                        }
                    }

                    Device updatedDevice = deviceRepository.save(device);
                    log.info("设备更新成功: {}", deviceId);
                    return updatedDevice;
                });
    }

    /**
     * 根据企业ID查找设备（带权限控制）
     */
    public Page<Device> findByEnterpriseId(Long enterpriseId, Pageable pageable) {
        return deviceRepository.findByEnterpriseId(enterpriseId, pageable);
    }

    /**
     * 多条件搜索设备（分页）
     *
     * @param keyword 搜索关键词（设备名称或设备ID）
     * @param status 设备状态筛选
     * @param userRole 用户角色
     * @param userEnterpriseId 用户企业ID
     * @param page 分页参数
     * @return 搜索结果
     */
    @Transactional(readOnly = true)
    public Page<Device> searchDevices(String keyword, String status, String userRole, Long enterpriseId,
                                        int page, int size, String sortBy, String sortDir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        log.info("执行设备搜索 - keyword: {}, status: {}, enterpriseId: {}, userRole: {}",
                keyword, status, enterpriseId, userRole);

        return deviceRepository.searchDevices(keyword, status, enterpriseId, pageable);
    }

    /**
     * 多条件搜索设备（不分页）
     *
     * @param keyword 搜索关键词（设备名称或设备ID）
     * @param status 设备状态筛选
     * @param userRole 用户角色
     * @param userEnterpriseId 用户企业ID
     * @return 搜索结果
     */
    @Transactional(readOnly = true)
    public List<Device> searchDevices(String keyword, String status, String userRole, Long enterpriseId) {

        log.info("执行设备搜索(不分页) - keyword: {}, status: {}, enterpriseId: {}, userRole: {}",
                keyword, status, enterpriseId, userRole);

        return deviceRepository.searchDevices(keyword, status, enterpriseId);
    }

    /**
     * 删除设备（软删除）
     */
    @Transactional
    public boolean deleteDevice(String deviceId, Long enterpriseId) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(device -> {
                    // 检查企业权限
                    if (enterpriseId != null && !device.getEnterpriseId().equals(enterpriseId)) {
                        throw new IllegalArgumentException("无权限删除此设备");
                    }

                    device.setDeleted(true);
                    deviceRepository.save(device);
                    log.info("设备已删除: {}", deviceId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 获取设备详细信息
     *
     * @param deviceId 设备ID
     * @param userEnterpriseId 用户企业ID
     * @return 设备详细信息
     */
    @Transactional(readOnly = true)
    public Optional<DeviceDetailResponse> getDeviceDetail(String deviceId, Long userEnterpriseId) {
        log.info("获取设备详细信息: deviceId={}, userEnterpriseId={}", deviceId, userEnterpriseId);

        return deviceRepository.findByDeviceId(deviceId)
                .map(device -> {
                    // 检查权限
                    if (userEnterpriseId != null && !device.getEnterpriseId().equals(userEnterpriseId)) {
                        log.warn("用户无权限访问设备: deviceId={}, userEnterpriseId={}, deviceEnterpriseId={}",
                                deviceId, userEnterpriseId, device.getEnterpriseId());
                        return Optional.<DeviceDetailResponse>empty();
                    }

                    // 构建设备详情响应
                    DeviceDetailResponse response = DeviceDetailResponse.fromSimpleDevice(device);

                    // 添加规格信息（示例数据，后续可以从数据库获取）
                    response.setSpecifications(DeviceSpecifications.builder()
                            .model("标准型号")
                            .manufacturer("设备制造商")
                            .version("v1.0.0")
                            .serialNumber("SN" + deviceId)
                            .installDate(device.getCreatedAt())
                            .warrantyPeriod("2年")
                            .build());

                    // 添加实时数据（模拟数据，后续从MQTT或时序数据库获取）
                    response.setRealTimeData(DeviceRealTimeData.builder()
                            .temperature(25.5)
                            .humidity(65.2)
                            .pressure(101325.0)
                            .power(12.5)
                            .signalStrength(-45)
                            .lastDataTime(LocalDateTime.now().minusMinutes(1))
                            .dataInterval(60)
                            .build());

                    log.info("成功获取设备详细信息: {}", deviceId);
                    return Optional.of(response);
                })
                .orElse(Optional.empty());
    }

    /**
     * 获取设备历史记录
     *
     * @param deviceId 设备ID
     * @param userEnterpriseId 用户企业ID
     * @param page 页码
     * @param size 页大小
     * @return 历史记录分页数据
     */
    @Transactional(readOnly = true)
    public Page<DeviceHistoryRecord> getDeviceHistory(String deviceId, Long userEnterpriseId, int page, int size) {
        log.info("获取设备历史记录: deviceId={}, page={}, size={}", deviceId, page, size);

        // 检查设备权限
        Optional<Device> device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            log.warn("设备不存在: {}", deviceId);
            return Page.empty();
        }

        if (userEnterpriseId != null && !device.get().getEnterpriseId().equals(userEnterpriseId)) {
            log.warn("用户无权限访问设备历史记录: {}", deviceId);
            return Page.empty();
        }

        // 生成模拟历史记录（后续从数据库获取）
        List<DeviceHistoryRecord> records = generateMockHistoryRecords(deviceId);

        // 分页处理
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordTime"));
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), records.size());

        if (start >= records.size()) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, records.size());
        }

        List<DeviceHistoryRecord> pageContent = records.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, records.size());
    }

    /**
     * 获取设备告警信息
     *
     * @param deviceId 设备ID
     * @param userEnterpriseId 用户企业ID
     * @return 告警列表
     */
    @Transactional(readOnly = true)
    public List<DeviceAlert> getDeviceAlerts(String deviceId, Long userEnterpriseId) {
        log.info("获取设备告警信息: deviceId={}", deviceId);

        // 检查设备权限
        Optional<Device> device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            log.warn("设备不存在: {}", deviceId);
            return List.of();
        }

        if (userEnterpriseId != null && !device.get().getEnterpriseId().equals(userEnterpriseId)) {
            log.warn("用户无权限访问设备告警信息: {}", deviceId);
            return List.of();
        }

        // 生成模拟告警数据（后续从数据库获取）
        return generateMockAlerts(deviceId);
    }

    /**
     * 获取设备实时数据
     *
     * @param deviceId 设备ID
     * @param userEnterpriseId 用户企业ID
     * @return 实时数据
     */
    @Transactional(readOnly = true)
    public Optional<DeviceRealTimeData> getDeviceRealTimeData(String deviceId, Long userEnterpriseId) {
        log.info("获取设备实时数据: deviceId={}", deviceId);

        // 检查设备权限
        Optional<Device> device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            log.warn("设备不存在: {}", deviceId);
            return Optional.empty();
        }

        if (userEnterpriseId != null && !device.get().getEnterpriseId().equals(userEnterpriseId)) {
            log.warn("用户无权限访问设备实时数据: {}", deviceId);
            return Optional.empty();
        }

        // 从设备传感器数据表获取最新实时数据
        Optional<DeviceSensorData> latestData = deviceSensorDataRepository
                .findTopByDeviceIdOrderByTimestampDesc(deviceId);

        if (latestData.isPresent()) {
            DeviceSensorData data = latestData.get();
            DeviceRealTimeData realTimeData = DeviceRealTimeData.builder()
                    .temperature(data.getTemperature())
                    .humidity(data.getHumidity())
                    .pressure(data.getPressure())
                    .power(data.getPower())
                    .signalStrength(data.getSignalStrength())
                    .lastDataTime(data.getTimestamp())
                    .dataInterval(60)
                    .build();

            log.debug("成功获取设备实时数据: deviceId={}, temperature={}", deviceId, data.getTemperature());
            return Optional.of(realTimeData);
        } else {
            log.warn("设备暂无传感器数据: {}", deviceId);
            return Optional.empty();
        }
    }

    /**
     * 生成模拟历史记录
     */
    private List<DeviceHistoryRecord> generateMockHistoryRecords(String deviceId) {
        return List.of(
                DeviceHistoryRecord.builder()
                        .id(1L)
                        .deviceId(deviceId)
                        .recordType("status")
                        .recordTime(LocalDateTime.now().minusHours(2))
                        .content("设备状态从离线变为在线")
                        .operator("系统自动")
                        .build(),
                DeviceHistoryRecord.builder()
                        .id(2L)
                        .deviceId(deviceId)
                        .recordType("data")
                        .recordTime(LocalDateTime.now().minusHours(1))
                        .content("上报传感器数据")
                        .operator("设备自动")
                        .build(),
                DeviceHistoryRecord.builder()
                        .id(3L)
                        .deviceId(deviceId)
                        .recordType("maintenance")
                        .recordTime(LocalDateTime.now().minusDays(7))
                        .content("完成例行维护检查")
                        .operator("维护工程师")
                        .build()
        );
    }

    /**
     * 生成模拟告警数据
     */
    private List<DeviceAlert> generateMockAlerts(String deviceId) {
        return List.of(
                DeviceAlert.builder()
                        .id(1L)
                        .deviceId(deviceId)
                        .alertType("temperature")
                        .alertLevel("MEDIUM")
                        .alertTitle("温度异常")
                        .alertMessage("设备温度超过正常范围")
                        .alertTime(LocalDateTime.now().minusMinutes(30))
                        .status("ACTIVE")
                        .build(),
                DeviceAlert.builder()
                        .id(2L)
                        .deviceId(deviceId)
                        .alertType("connection")
                        .alertLevel("LOW")
                        .alertTitle("连接不稳定")
                        .alertMessage("设备网络连接时断时续")
                        .alertTime(LocalDateTime.now().minusHours(1))
                        .status("RESOLVED")
                        .resolvedTime(LocalDateTime.now().minusMinutes(45))
                        .resolvedBy("系统自动")
                        .build()
        );
    }

    /**
     * 设备统计数据
     */
    public static class DeviceStats {
        private final long totalCount;
        private final long onlineCount;
        private final long offlineCount;

        public DeviceStats(long totalCount, long onlineCount, long offlineCount) {
            this.totalCount = totalCount;
            this.onlineCount = onlineCount;
            this.offlineCount = offlineCount;
        }

        public long getTotalCount() { return totalCount; }
        public long getOnlineCount() { return onlineCount; }
        public long getOfflineCount() { return offlineCount; }
        public double getOnlineRate() {
            return totalCount > 0 ? (double) onlineCount / totalCount * 100 : 0;
        }
    }

    // ==================== 设备在线状态委托方法 ====================

    /**
     * 获取设备在线状态
     * 委托给DeviceOnlineStatusService
     */
    public DeviceOnlineStatusService.DeviceOnlineStatus getDeviceOnlineStatus(String deviceId) {
        return deviceOnlineStatusService.getDeviceOnlineStatus(deviceId);
    }
}