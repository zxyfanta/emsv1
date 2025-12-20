package com.ems.service;

import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceGroup;
import com.ems.entity.enterprise.Enterprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 设备分组服务接口
 *
 * @author EMS Team
 */
public interface DeviceGroupService {

    /**
     * 创建设备分组
     */
    DeviceGroup createGroup(DeviceGroup deviceGroup);

    /**
     * 更新设备分组
     */
    DeviceGroup updateGroup(DeviceGroup deviceGroup);

    /**
     * 删除设备分组
     */
    void deleteGroup(Long groupId);

    /**
     * 根据ID获取设备分组
     */
    DeviceGroup getGroupById(Long groupId);

    /**
     * 获取企业下的所有设备分组
     */
    List<DeviceGroup> getGroupsByEnterprise(Enterprise enterprise);

    /**
     * 获取企业下的设备分组（分页）
     */
    Page<DeviceGroup> getGroupsByEnterprise(Enterprise enterprise, Pageable pageable);

    /**
     * 获取根分组
     */
    List<DeviceGroup> getRootGroups(Enterprise enterprise);

    /**
     * 获取子分组
     */
    List<DeviceGroup> getChildGroups(Long parentGroupId);

    /**
     * 向分组添加设备
     */
    void addDevicesToGroup(Long groupId, List<Long> deviceIds);

    /**
     * 从分组移除设备
     */
    void removeDevicesFromGroup(Long groupId, List<Long> deviceIds);

    /**
     * 获取分组下的设备
     */
    List<Device> getDevicesInGroup(Long groupId);

    /**
     * 搜索设备分组
     */
    Page<DeviceGroup> searchGroups(Enterprise enterprise, String keyword, Pageable pageable);

    /**
     * 检查分组名称是否可用
     */
    boolean isNameAvailable(String name, Enterprise enterprise, Long excludeId);

    /**
     * 移动分组到新的父分组
     */
    void moveGroup(Long groupId, Long newParentGroupId);

    /**
     * 获取分组统计信息
     */
    Map<String, Object> getGroupStatistics(Enterprise enterprise);

    /**
     * 批量操作分组
     */
    void batchUpdateGroupStatus(List<Long> groupIds, boolean enabled);

    /**
     * 获取分组的完整路径
     */
    String getGroupPath(Long groupId);

    /**
     * 根据分组类型获取分组
     */
    List<DeviceGroup> getGroupsByType(DeviceGroup.GroupType groupType, Enterprise enterprise);

    /**
     * 获取未分配的设备
     */
    List<Device> getUnassignedDevices(Enterprise enterprise, Long excludeGroupId);

    /**
     * 验证分组删除前的约束
     */
    boolean canDeleteGroup(Long groupId);
}