package com.ems.service;

import com.ems.entity.DeviceType;
import com.ems.repository.DeviceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 设备类型管理服务
 * 提供设备类型的增删改查和启用/禁用功能
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;

    /**
     * 获取所有设备类型
     */
    @Transactional(readOnly = true)
    public List<DeviceType> getAllDeviceTypes() {
        return deviceTypeRepository.findAll();
    }

    /**
     * 获取所有启用的设备类型
     */
    @Transactional(readOnly = true)
    public List<DeviceType> getEnabledDeviceTypes() {
        List<DeviceType> enabledTypes = deviceTypeRepository.findByEnabledTrue();
        log.debug("获取到{}个启用的设备类型", enabledTypes.size());
        return enabledTypes;
    }

    /**
     * 根据类型代码获取设备类型
     */
    @Transactional(readOnly = true)
    public Optional<DeviceType> getDeviceTypeByCode(String typeCode) {
        return deviceTypeRepository.findByTypeCode(typeCode);
    }

    /**
     * 根据表名获取设备类型
     */
    @Transactional(readOnly = true)
    public Optional<DeviceType> getDeviceTypeByTableName(String tableName) {
        return deviceTypeRepository.findByTableName(tableName);
    }

    /**
     * 根据设备ID获取设备类型（通过设备表的device_type字段）
     */
    @Transactional(readOnly = true)
    public DeviceType getDeviceTypeByDeviceId(String deviceId) {
        // 这里需要根据设备ID从设备表中获取设备类型
        // 暂时使用默认逻辑，后续与DeviceService集成
        return getDeviceTypeByCode("RADIATION").orElse(null);
    }

    /**
     * 启用或禁用设备类型
     */
    public DeviceType toggleDeviceType(String typeCode, boolean enabled) {
        DeviceType deviceType = deviceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("设备类型不存在: " + typeCode));

        if (!deviceType.getEnabled().equals(enabled)) {
            deviceType.setEnabled(enabled);
            deviceType = deviceTypeRepository.save(deviceType);

            String action = enabled ? "启用" : "禁用";
            log.info("设备类型已{}: {}", action, deviceType.getFullDescription());
        }

        return deviceType;
    }

    /**
     * 创建设备类型
     */
    public DeviceType createDeviceType(DeviceType deviceType) {
        // 检查类型代码是否已存在
        if (deviceTypeRepository.existsByTypeCode(deviceType.getTypeCode())) {
            throw new IllegalArgumentException("设备类型代码已存在: " + deviceType.getTypeCode());
        }

        // 检查表名是否已存在
        if (deviceTypeRepository.findByTableName(deviceType.getTableName()).isPresent()) {
            throw new IllegalArgumentException("数据表名已被使用: " + deviceType.getTableName());
        }

        // 设置默认值
        if (deviceType.getEnabled() == null) {
            deviceType.setEnabled(true);
        }

        DeviceType savedDeviceType = deviceTypeRepository.save(deviceType);
        log.info("设备类型创建成功: {}", savedDeviceType.getFullDescription());

        return savedDeviceType;
    }

    /**
     * 更新设备类型
     */
    public DeviceType updateDeviceType(String typeCode, DeviceType deviceTypeUpdates) {
        DeviceType existingDeviceType = deviceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("设备类型不存在: " + typeCode));

        // 更新可修改的字段
        if (deviceTypeUpdates.getTypeName() != null) {
            existingDeviceType.setTypeName(deviceTypeUpdates.getTypeName());
        }
        if (deviceTypeUpdates.getDescription() != null) {
            existingDeviceType.setDescription(deviceTypeUpdates.getDescription());
        }
        if (deviceTypeUpdates.getMqttTopicPattern() != null) {
            existingDeviceType.setMqttTopicPattern(deviceTypeUpdates.getMqttTopicPattern());
        }
        if (deviceTypeUpdates.getEnabled() != null) {
            existingDeviceType.setEnabled(deviceTypeUpdates.getEnabled());
        }

        DeviceType updatedDeviceType = deviceTypeRepository.save(existingDeviceType);
        log.info("设备类型更新成功: {}", updatedDeviceType.getFullDescription());

        return updatedDeviceType;
    }

    /**
     * 删除设备类型
     */
    public void deleteDeviceType(String typeCode) {
        DeviceType deviceType = deviceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("设备类型不存在: " + typeCode));

        // 检查是否有关联的设备
        // TODO: 添加设备关联检查逻辑

        deviceTypeRepository.delete(deviceType);
        log.info("设备类型已删除: {}", deviceType.getFullDescription());
    }

    /**
     * 初始化默认设备类型
     */
    public void initializeDefaultDeviceTypes() {
        log.info("开始初始化默认设备类型...");

        Arrays.stream(DeviceType.Type.values())
                .forEach(type -> {
                    if (!deviceTypeRepository.existsByTypeCode(type.getCode())) {
                        DeviceType deviceType = DeviceType.builder()
                                .typeCode(type.getCode())
                                .typeName(type.getDisplayName())
                                .tableName(type.getTableName())
                                .enabled(type.isDefaultEnabled())
                                .description(String.format("系统默认的%s", type.getDisplayName()))
                                .mqttTopicPattern(type.getMqttTopicPattern()) // 使用枚举中定义的主题模式
                                .build();

                        deviceTypeRepository.save(deviceType);
                        log.info("默认设备类型已创建: {} - MQTT主题: {}",
                                deviceType.getFullDescription(), deviceType.getMqttTopicPattern());
                    } else {
                        log.debug("设备类型已存在，跳过创建: {}", type.getCode());
                    }
                });

        log.info("默认设备类型初始化完成");
    }

    /**
     * 根据MQTT主题查找匹配的设备类型
     */
    @Transactional(readOnly = true)
    public Optional<DeviceType> findDeviceTypeByTopic(String topic) {
        return getEnabledDeviceTypes().stream()
                .filter(deviceType -> deviceType.matchesTopic(topic))
                .findFirst();
    }

    /**
     * 搜索设备类型
     */
    @Transactional(readOnly = true)
    public List<DeviceType> searchDeviceTypes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllDeviceTypes();
        }

        return deviceTypeRepository.findByKeyword(keyword.trim());
    }

    /**
     * 获取设备类型统计信息
     */
    @Transactional(readOnly = true)
    public DeviceTypeStatistics getStatistics() {
        long totalCount = deviceTypeRepository.count();
        long enabledCount = deviceTypeRepository.countEnabledDeviceTypes();
        long disabledCount = totalCount - enabledCount;

        return new DeviceTypeStatistics(totalCount, enabledCount, disabledCount);
    }

    /**
     * 生成MQTT主题模式
     * 使用方案一主题结构: ems/device/{deviceId}/data/{deviceType}
     */
    private String generateMqttTopicPattern(String typeCode) {
        // 方案一: ems/device/{deviceId}/data/{deviceType}
        return "ems/device/+/data/" + typeCode;
    }

    /**
     * 设备类型统计信息
     */
    public static class DeviceTypeStatistics {
        private final long totalCount;
        private final long enabledCount;
        private final long disabledCount;

        public DeviceTypeStatistics(long totalCount, long enabledCount, long disabledCount) {
            this.totalCount = totalCount;
            this.enabledCount = enabledCount;
            this.disabledCount = disabledCount;
        }

        public long getTotalCount() { return totalCount; }
        public long getEnabledCount() { return enabledCount; }
        public long getDisabledCount() { return disabledCount; }

        @Override
        public String toString() {
            return String.format("DeviceTypeStatistics{total=%d, enabled=%d, disabled=%d}",
                               totalCount, enabledCount, disabledCount);
        }
    }
}