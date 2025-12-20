package com.ems.service.impl.device;

import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceGroup;
import com.ems.entity.device.DeviceGroupMapping;
import com.ems.entity.enterprise.Enterprise;
import com.ems.repository.device.DeviceGroupMappingRepository;
import com.ems.repository.device.DeviceGroupRepository;
import com.ems.repository.device.DeviceRepository;
import com.ems.service.DeviceCacheService;
import com.ems.service.DeviceGroupService;
import com.ems.service.RedisCacheService;
import com.ems.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备分组服务实现
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceGroupServiceImpl implements DeviceGroupService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceGroupMappingRepository mappingRepository;
    private final DeviceCacheService deviceCacheService;
    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public DeviceGroup createGroup(DeviceGroup deviceGroup) {
        log.info("创建设备分组: {}", deviceGroup.getName());

        // 验证分组名称唯一性
        if (!isNameAvailable(deviceGroup.getName(), deviceGroup.getEnterprise(), null)) {
            throw new IllegalArgumentException("分组名称已存在: " + deviceGroup.getName());
        }

        // 设置默认值
        if (deviceGroup.getSortOrder() == null) {
            deviceGroup.setSortOrder(0);
        }
        if (deviceGroup.getColor() == null) {
            deviceGroup.setColor("#409EFF");
        }

        return deviceGroupRepository.save(deviceGroup);
    }

    @Override
    @Transactional
    public DeviceGroup updateGroup(DeviceGroup deviceGroup) {
        log.info("更新设备分组: {}", deviceGroup.getId());

        DeviceGroup existingGroup = getGroupById(deviceGroup.getId());

        // 验证分组名称唯一性（排除自己）
        if (!isNameAvailable(deviceGroup.getName(), deviceGroup.getEnterprise(), deviceGroup.getId())) {
            throw new IllegalArgumentException("分组名称已存在: " + deviceGroup.getName());
        }

        // 更新允许的字段
        existingGroup.setName(deviceGroup.getName());
        existingGroup.setDescription(deviceGroup.getDescription());
        existingGroup.setColor(deviceGroup.getColor());
        existingGroup.setSortOrder(deviceGroup.getSortOrder());
        existingGroup.setEnabled(deviceGroup.getEnabled());

        return deviceGroupRepository.save(existingGroup);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        log.info("删除设备分组: {}", groupId);

        DeviceGroup deviceGroup = getGroupById(groupId);

        // 验证是否可以删除
        if (!canDeleteGroup(groupId)) {
            throw new IllegalStateException("无法删除包含子分组或设备的分组");
        }

        // 软删除
        deviceGroup.setDeleted(true);
        deviceGroupRepository.save(deviceGroup);

        // 同步清理缓存
        redisCacheService.syncGroupDeletion(groupId);
        deviceCacheService.removeGroupDevicesCache(groupId);
    }

    @Override
    public DeviceGroup getGroupById(Long groupId) {
        return deviceGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("设备分组不存在: " + groupId));
    }

    @Override
    public List<DeviceGroup> getGroupsByEnterprise(Enterprise enterprise) {
        return deviceGroupRepository.findGroupsByEnterprise(enterprise,
            org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
    }

    @Override
    public Page<DeviceGroup> getGroupsByEnterprise(Enterprise enterprise, Pageable pageable) {
        return deviceGroupRepository.findGroupsByEnterprise(enterprise, pageable);
    }

    @Override
    public List<DeviceGroup> getRootGroups(Enterprise enterprise) {
        return deviceGroupRepository.findRootGroupsByEnterprise(enterprise);
    }

    @Override
    public List<DeviceGroup> getChildGroups(Long parentGroupId) {
        DeviceGroup parentGroup = getGroupById(parentGroupId);
        return deviceGroupRepository.findByParentGroup(parentGroup);
    }

    @Override
    @Transactional
    public void addDevicesToGroup(Long groupId, List<Long> deviceIds) {
        log.info("向分组 {} 添加设备: {}", groupId, deviceIds);

        DeviceGroup deviceGroup = getGroupById(groupId);
        List<Device> devices = deviceRepository.findAllById(deviceIds);

        // 过滤出属于同一企业的设备
        List<Device> validDevices = devices.stream()
                .filter(device -> Objects.equals(device.getEnterprise(), deviceGroup.getEnterprise()))
                .collect(Collectors.toList());

        for (Device device : validDevices) {
            // 检查是否已存在映射关系
            if (!mappingRepository.findByDeviceIdAndGroupIdAndEnterpriseId(
                    device.getId(), groupId, deviceGroup.getEnterprise().getId()).isPresent()) {

                DeviceGroupMapping mapping = DeviceGroupMapping.builder()
                        .device(device)
                        .deviceGroup(deviceGroup)
                        .enterprise(deviceGroup.getEnterprise())
                        .enabled(true)
                        .build();
                mappingRepository.save(mapping);

                // 同步更新缓存
                deviceCacheService.addDeviceToGroupCache(groupId, device.getId());
                redisCacheService.syncDeviceGroupChange(device.getId(), groupId, true);
            }
        }
    }

    @Override
    @Transactional
    public void removeDevicesFromGroup(Long groupId, List<Long> deviceIds) {
        log.info("从分组 {} 移除设备: {}", groupId, deviceIds);

        DeviceGroup deviceGroup = getGroupById(groupId);

        for (Long deviceId : deviceIds) {
            mappingRepository.disableByDeviceIdAndGroupIdAndEnterpriseId(
                    deviceId, groupId, deviceGroup.getEnterprise().getId());

            // 同步更新缓存
            deviceCacheService.removeDeviceFromGroupCache(groupId, deviceId);
            redisCacheService.syncDeviceGroupChange(deviceId, groupId, false);
        }
    }

    @Override
    public List<Device> getDevicesInGroup(Long groupId) {
        DeviceGroup deviceGroup = getGroupById(groupId);

        // 优先从缓存获取设备ID列表
        List<Long> deviceIds = deviceCacheService.getDevicesInGroup(groupId);

        if (deviceIds.isEmpty()) {
            return new ArrayList<>();
        }

        return deviceRepository.findAllById(deviceIds);
    }

    @Override
    public Page<DeviceGroup> searchGroups(Enterprise enterprise, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getGroupsByEnterprise(enterprise, pageable);
        }
        return deviceGroupRepository.searchGroups(enterprise, keyword.trim(), pageable);
    }

    @Override
    public boolean isNameAvailable(String name, Enterprise enterprise, Long excludeId) {
        if (excludeId != null) {
            return !deviceGroupRepository.existsByNameAndEnterprise(name, enterprise, excludeId);
        } else {
            return !deviceGroupRepository.existsByNameAndEnterprise(name, enterprise);
        }
    }

    @Override
    @Transactional
    public void moveGroup(Long groupId, Long newParentGroupId) {
        log.info("移动分组 {} 到新的父分组 {}", groupId, newParentGroupId);

        DeviceGroup deviceGroup = getGroupById(groupId);
        DeviceGroup newParentGroup = newParentGroupId != null ? getGroupById(newParentGroupId) : null;

        // 检查是否会形成循环引用
        if (newParentGroup != null && wouldCreateCycle(deviceGroup, newParentGroup)) {
            throw new IllegalArgumentException("不能将分组移动到其子分组中");
        }

        // 检查是否属于同一企业
        if (newParentGroup != null && !Objects.equals(deviceGroup.getEnterprise(), newParentGroup.getEnterprise())) {
            throw new IllegalArgumentException("只能在同一企业内移动分组");
        }

        deviceGroup.setParentGroup(newParentGroup);
        deviceGroupRepository.save(deviceGroup);
    }

    /**
     * 检查是否会形成循环引用
     */
    private boolean wouldCreateCycle(DeviceGroup group, DeviceGroup newParent) {
        DeviceGroup current = newParent;
        while (current != null) {
            if (current.equals(group)) {
                return true;
            }
            current = current.getParentGroup();
        }
        return false;
    }

    @Override
    public Map<String, Object> getGroupStatistics(Enterprise enterprise) {
        Map<String, Object> statistics = new HashMap<>();

        // 总分组数
        long totalGroups = deviceGroupRepository.countByEnterprise(enterprise);
        statistics.put("totalGroups", totalGroups);

        // 按类型统计
        List<Object[]> typeStats = deviceGroupRepository.countByGroupType(enterprise);
        Map<String, Long> groupTypeStats = new HashMap<>();
        for (Object[] stat : typeStats) {
            DeviceGroup.GroupType type = (DeviceGroup.GroupType) stat[0];
            Long count = (Long) stat[1];
            groupTypeStats.put(type.name(), count);
        }
        statistics.put("groupTypeStats", groupTypeStats);

        // 启用分组数
        List<DeviceGroup> enabledGroups = deviceGroupRepository.findEnabledGroupsByEnterprise(enterprise);
        statistics.put("enabledGroups", enabledGroups.size());

        // 禁用分组数
        statistics.put("disabledGroups", totalGroups - enabledGroups.size());

        // 根分组数
        List<DeviceGroup> rootGroups = deviceGroupRepository.findRootGroupsByEnterprise(enterprise);
        statistics.put("rootGroups", rootGroups.size());

        return statistics;
    }

    @Override
    @Transactional
    public void batchUpdateGroupStatus(List<Long> groupIds, boolean enabled) {
        log.info("批量更新分组状态: {} -> {}", groupIds, enabled);

        List<DeviceGroup> groups = deviceGroupRepository.findAllById(groupIds);
        for (DeviceGroup group : groups) {
            group.setEnabled(enabled);
        }
        deviceGroupRepository.saveAll(groups);
    }

    @Override
    public String getGroupPath(Long groupId) {
        DeviceGroup deviceGroup = getGroupById(groupId);
        return deviceGroup.getGroupPath();
    }

    @Override
    public List<DeviceGroup> getGroupsByType(DeviceGroup.GroupType groupType, Enterprise enterprise) {
        return deviceGroupRepository.findByGroupTypeAndEnterprise(groupType, enterprise);
    }

    @Override
    public List<Device> getUnassignedDevices(Enterprise enterprise, Long excludeGroupId) {
        List<Long> unassignedDeviceIds = mappingRepository.findUnassignedDeviceIdsByEnterpriseId(enterprise.getId());

        if (excludeGroupId != null) {
            List<Long> excludeDeviceIds = mappingRepository.findDeviceIdsByGroupIdAndEnterpriseId(
                    excludeGroupId, enterprise.getId());
            unassignedDeviceIds.addAll(excludeDeviceIds);
        }

        if (unassignedDeviceIds.isEmpty()) {
            return new ArrayList<>();
        }

        return deviceRepository.findAllById(unassignedDeviceIds);
    }

    @Override
    public boolean canDeleteGroup(Long groupId) {
        DeviceGroup deviceGroup = getGroupById(groupId);

        // 检查是否有子分组
        if (deviceGroupRepository.hasChildGroups(deviceGroup)) {
            return false;
        }

        // 检查是否有设备
        long deviceCount = mappingRepository.countByGroupIdAndEnterpriseId(
                groupId, deviceGroup.getEnterprise().getId());
        if (deviceCount > 0) {
            return false;
        }

        return true;
    }

    /**
     * 获取当前用户的企业
     */
    private Enterprise getCurrentUserEnterprise() {
        Long enterpriseId = SecurityUtils.getCurrentUserEnterpriseId();
        if (enterpriseId == null) {
            throw new IllegalStateException("用户未关联企业");
        }
        return Enterprise.builder().id(enterpriseId).build();
    }
}