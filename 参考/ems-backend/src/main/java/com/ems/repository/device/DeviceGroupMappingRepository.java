package com.ems.repository.device;

import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceGroup;
import com.ems.entity.device.DeviceGroupMapping;
import com.ems.entity.enterprise.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 设备分组映射数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface DeviceGroupMappingRepository extends JpaRepository<DeviceGroupMapping, Long> {

    /**
     * 根据设备查找分组映射
     */
    List<DeviceGroupMapping> findByDevice(Device device);

    /**
     * 根据设备ID查找分组映射
     */
    @Query("SELECT dgm FROM DeviceGroupMapping dgm WHERE dgm.device.id = :deviceId AND dgm.enabled = true")
    List<DeviceGroupMapping> findByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 根据分组查找设备映射
     */
    List<DeviceGroupMapping> findByDeviceGroup(DeviceGroup deviceGroup);

    /**
     * 根据分组ID查找设备映射
     */
    @Query("SELECT dgm FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enabled = true")
    List<DeviceGroupMapping> findByGroupId(@Param("groupId") Long groupId);

    /**
     * 根据企业和设备查找分组映射
     */
    @Query("SELECT dgm FROM DeviceGroupMapping dgm WHERE dgm.device = :device AND dgm.enterprise = :enterprise AND dgm.enabled = true")
    List<DeviceGroupMapping> findByDeviceAndEnterprise(@Param("device") Device device, @Param("enterprise") Enterprise enterprise);

    /**
     * 根据企业和分组查找设备映射
     */
    @Query("SELECT dgm FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup = :deviceGroup AND dgm.enterprise = :enterprise AND dgm.enabled = true")
    List<DeviceGroupMapping> findByDeviceGroupAndEnterprise(@Param("deviceGroup") DeviceGroup deviceGroup, @Param("enterprise") Enterprise enterprise);

    /**
     * 检查设备是否在指定分组中
     */
    @Query("SELECT dgm FROM DeviceGroupMapping dgm WHERE dgm.device.id = :deviceId AND dgm.deviceGroup.id = :groupId AND dgm.enabled = true")
    Optional<DeviceGroupMapping> findByDeviceIdAndGroupId(@Param("deviceId") Long deviceId, @Param("groupId") Long groupId);

    /**
     * 检查设备和企业是否在指定分组中
     */
    @Query("SELECT dgm FROM DeviceGroupMapping dgm WHERE dgm.device.id = :deviceId AND dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true")
    Optional<DeviceGroupMapping> findByDeviceIdAndGroupIdAndEnterpriseId(@Param("deviceId") Long deviceId, @Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId);

    /**
     * 统计分组中的设备数量
     */
    @Query("SELECT COUNT(dgm) FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enabled = true")
    long countByGroupId(@Param("groupId") Long groupId);

    /**
     * 统计企业分组中的设备数量
     */
    @Query("SELECT COUNT(dgm) FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true")
    long countByGroupIdAndEnterpriseId(@Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId);

    /**
     * 获取分组中的所有设备ID
     */
    @Query("SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enabled = true")
    List<Long> findDeviceIdsByGroupId(@Param("groupId") Long groupId);

    /**
     * 获取企业和分组中的所有设备ID
     */
    @Query("SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true")
    List<Long> findDeviceIdsByGroupIdAndEnterpriseId(@Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId);

    /**
     * 获取企业中未分配到任何分组的设备ID
     */
    @Query("SELECT d.id FROM Device d WHERE d.enterprise.id = :enterpriseId AND d.deleted = false AND " +
           "d.id NOT IN (SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.device.id = d.id AND dgm.enabled = true)")
    List<Long> findUnassignedDeviceIdsByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 根据设备ID和企业ID查找所有分组映射
     */
    @Query("SELECT dgm.deviceGroup.id FROM DeviceGroupMapping dgm WHERE dgm.device.id = :deviceId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true")
    List<Long> findGroupIdsByDeviceIdAndEnterpriseId(@Param("deviceId") Long deviceId, @Param("enterpriseId") Long enterpriseId);

    /**
     * 删除设备的所有分组映射
     */
    @Query("UPDATE DeviceGroupMapping dgm SET dgm.enabled = false WHERE dgm.device.id = :deviceId")
    void disableByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 删除分组的所有设备映射
     */
    @Query("UPDATE DeviceGroupMapping dgm SET dgm.enabled = false WHERE dgm.deviceGroup.id = :groupId")
    void disableByGroupId(@Param("groupId") Long groupId);

    /**
     * 删除企业和分组中设备的所有映射
     */
    @Query("UPDATE DeviceGroupMapping dgm SET dgm.enabled = false WHERE dgm.device.id = :deviceId AND dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId")
    void disableByDeviceIdAndGroupIdAndEnterpriseId(@Param("deviceId") Long deviceId, @Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId);
}