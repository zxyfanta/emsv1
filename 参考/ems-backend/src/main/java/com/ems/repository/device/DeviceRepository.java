package com.ems.repository.device;

import com.ems.entity.device.Device;
import com.ems.entity.device.DeviceGroup;
import com.ems.entity.enterprise.Enterprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备数据访问接口 - 简化版本
 *
 * @author EMS Team
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * 查找所有未删除的设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false ORDER BY d.createdAt DESC")
    Page<Device> findAllActive(Pageable pageable);

    /**
     * 根据设备ID查找设备
     */
    Optional<Device> findByDeviceId(String deviceId);

    /**
     * 根据设备ID查找设备（直接返回，用于聚合服务）
     */
    @Query("SELECT d FROM Device d WHERE d.deviceId = :deviceId AND d.deleted = false")
    Device findByDeviceIdDirect(@Param("deviceId") String deviceId);

    /**
     * 根据企业ID查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.enterprise.id = :enterpriseId AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Device> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 根据企业查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.enterprise = :enterprise AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Device> findByEnterprise(@Param("enterprise") Enterprise enterprise);

    /**
     * 根据企业ID分页查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.enterprise.id = :enterpriseId AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Device> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId, Pageable pageable);

    /**
     * 根据状态查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND d.status = :status ORDER BY d.lastOnlineAt DESC")
    List<Device> findByStatus(@Param("status") Device.DeviceStatus status);

    /**
     * 查找所有在线设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND d.status = 'ONLINE' ORDER BY d.lastOnlineAt DESC")
    List<Device> findOnlineDevices();

    /**
     * 查找所有离线设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND d.status = 'OFFLINE' ORDER BY d.lastOnlineAt DESC")
    List<Device> findOfflineDevices();

    /**
     * 查找所有离线设备（超过指定时间未上线）
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND d.lastOnlineAt < :threshold ORDER BY d.lastOnlineAt DESC")
    List<Device> findOfflineDevicesBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * 根据名称模糊搜索设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND d.deviceName LIKE %:keyword%")
    List<Device> findByDeviceNameContaining(@Param("keyword") String keyword);

    /**
     * 统计企业设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.enterprise.id = :enterpriseId AND d.deleted = false")
    long countByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 统计设备状态数量
     */
    @Query("SELECT d.status, COUNT(d) FROM Device d WHERE d.deleted = false GROUP BY d.status")
    List<Object[]> countDevicesByStatus();

    /**
     * 统计未删除的设备总数
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.deleted = false")
    long countByDeletedFalse();

    /**
     * 多条件搜索设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND " +
           "(:keyword IS NULL OR d.deviceName LIKE CONCAT('%', :keyword, '%') OR d.deviceId LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:status IS NULL OR CAST(d.status AS STRING) = :status) AND " +
           "(:enterpriseId IS NULL OR d.enterprise.id = :enterpriseId) " +
           "ORDER BY d.createdAt DESC")
    Page<Device> searchDevices(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("enterpriseId") Long enterpriseId,
        Pageable pageable
    );

    /**
     * 多条件搜索设备（不分页，用于导出等场景）
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND " +
           "(:keyword IS NULL OR d.deviceName LIKE CONCAT('%', :keyword, '%') OR d.deviceId LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:status IS NULL OR CAST(d.status AS STRING) = :status) AND " +
           "(:enterpriseId IS NULL OR d.enterprise.id = :enterpriseId) " +
           "ORDER BY d.createdAt DESC")
    List<Device> searchDevices(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("enterpriseId") Long enterpriseId
    );

    // ===== 设备分组相关查询方法 =====

    /**
     * 根据分组ID和企业ID查询设备（通过DeviceGroupMapping关联）
     */
    @Query("SELECT d FROM Device d WHERE d.id IN " +
           "(SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true) " +
           "AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Device> findByGroupIdAndEnterpriseId(@Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId);

    /**
     * 根据分组ID和企业ID分页查询设备
     */
    @Query("SELECT d FROM Device d WHERE d.id IN " +
           "(SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true) " +
           "AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Device> findByGroupIdAndEnterpriseId(@Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId, Pageable pageable);

    /**
     * 根据多个分组ID查询设备
     */
    @Query("SELECT d FROM Device d WHERE d.id IN " +
           "(SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id IN :groupIds AND dgm.enabled = true) " +
           "AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Device> findByGroupIds(@Param("groupIds") List<Long> groupIds);

    /**
     * 根据企业ID查询未分配到任何分组的设备
     */
    @Query("SELECT d FROM Device d WHERE d.enterprise.id = :enterpriseId AND d.deleted = false AND " +
           "d.id NOT IN (SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.device.id = d.id AND dgm.enabled = true)")
    List<Device> findUnassignedDevicesByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 统计分组中的设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.id IN " +
           "(SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enterprise.id = :enterpriseId AND dgm.enabled = true) " +
           "AND d.deleted = false")
    long countByGroupIdAndEnterpriseId(@Param("groupId") Long groupId, @Param("enterpriseId") Long enterpriseId);

    /**
     * 查找设备所属的所有分组ID
     */
    @Query("SELECT dgm.deviceGroup.id FROM DeviceGroupMapping dgm WHERE dgm.device.id = :deviceId AND dgm.enabled = true")
    List<Long> findGroupIdsByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 检查设备是否在指定分组中
     */
    @Query("SELECT COUNT(dgm) > 0 FROM DeviceGroupMapping dgm WHERE dgm.device.id = :deviceId AND dgm.deviceGroup.id = :groupId AND dgm.enabled = true")
    boolean existsDeviceInGroup(@Param("deviceId") Long deviceId, @Param("groupId") Long groupId);

    /**
     * 多条件搜索设备（包含分组过滤）
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND " +
           "(:keyword IS NULL OR d.deviceName LIKE CONCAT('%', :keyword, '%') OR d.deviceId LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:status IS NULL OR CAST(d.status AS STRING) = :status) AND " +
           "(:enterpriseId IS NULL OR d.enterprise.id = :enterpriseId) AND " +
           "(:groupIds IS NULL OR d.id IN (SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id IN :groupIds AND dgm.enabled = true)) " +
           "ORDER BY d.createdAt DESC")
    Page<Device> searchDevicesWithGroupFilter(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("enterpriseId") Long enterpriseId,
        @Param("groupIds") List<Long> groupIds,
        Pageable pageable
    );

    /**
     * 多条件搜索设备（包含分组过滤，不分页）
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND " +
           "(:keyword IS NULL OR d.deviceName LIKE CONCAT('%', :keyword, '%') OR d.deviceId LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:status IS NULL OR CAST(d.status AS STRING) = :status) AND " +
           "(:enterpriseId IS NULL OR d.enterprise.id = :enterpriseId) AND " +
           "(:groupIds IS NULL OR d.id IN (SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id IN :groupIds AND dgm.enabled = true)) " +
           "ORDER BY d.createdAt DESC")
    List<Device> searchDevicesWithGroupFilter(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("enterpriseId") Long enterpriseId,
        @Param("groupIds") List<Long> groupIds
    );
}