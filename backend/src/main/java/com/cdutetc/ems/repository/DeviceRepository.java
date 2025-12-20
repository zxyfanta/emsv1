package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
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
 * 设备数据访问接口
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * 根据设备编码查找设备
     */
    Optional<Device> findByDeviceCode(String deviceCode);

    /**
     * 检查设备编码是否存在
     */
    boolean existsByDeviceCode(String deviceCode);

    /**
     * 根据企业ID查找设备
     */
    List<Device> findByCompanyId(Long companyId);

    /**
     * 根据企业ID分页查询设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId")
    Page<Device> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * 根据设备类型查找设备
     */
    List<Device> findByDeviceType(DeviceType deviceType);

    /**
     * 根据设备状态查找设备
     */
    List<Device> findByStatus(DeviceStatus status);

    /**
     * 根据企业ID和设备类型查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND d.deviceType = :deviceType")
    List<Device> findByCompanyIdAndDeviceType(@Param("companyId") Long companyId, @Param("deviceType") DeviceType deviceType);

    /**
     * 根据企业ID和设备状态查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND d.status = :status")
    List<Device> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") DeviceStatus status);

    /**
     * 根据企业ID、设备类型和状态查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND d.deviceType = :deviceType AND d.status = :status")
    List<Device> findByCompanyIdAndDeviceTypeAndStatus(
            @Param("companyId") Long companyId,
            @Param("deviceType") DeviceType deviceType,
            @Param("status") DeviceStatus status);

    /**
     * 根据设备名称模糊查询
     */
    @Query("SELECT d FROM Device d WHERE d.deviceName LIKE %:deviceName%")
    Page<Device> findByDeviceNameContaining(@Param("deviceName") String deviceName, Pageable pageable);

    /**
     * 根据企业ID和设备名称模糊查询
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND d.deviceName LIKE %:deviceName%")
    Page<Device> findByCompanyIdAndDeviceNameContaining(
            @Param("companyId") Long companyId,
            @Param("deviceName") String deviceName,
            Pageable pageable);

    /**
     * 查找最近在线的设备
     */
    @Query("SELECT d FROM Device d WHERE d.lastOnlineAt >= :since")
    List<Device> findRecentlyOnlineDevices(@Param("since") LocalDateTime since);

    /**
     * 根据企业ID查找最近在线的设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND d.lastOnlineAt >= :since")
    List<Device> findRecentlyOnlineDevicesByCompany(
            @Param("companyId") Long companyId,
            @Param("since") LocalDateTime since);

    /**
     * 统计企业设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    /**
     * 根据设备类型统计企业设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.company.id = :companyId AND d.deviceType = :deviceType")
    long countByCompanyIdAndDeviceType(
            @Param("companyId") Long companyId,
            @Param("deviceType") DeviceType deviceType);

    /**
     * 统计在线设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.status = 'ONLINE'")
    long countOnlineDevices();

    /**
     * 统计企业在线设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.company.id = :companyId AND d.status = 'ONLINE'")
    long countOnlineDevicesByCompany(@Param("companyId") Long companyId);

    /**
     * 根据企业ID和设备ID查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.id = :id AND d.company.id = :companyId")
    Optional<Device> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    /**
     * 根据企业ID和设备类型分页查询设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND d.deviceType = :deviceType")
    Page<Device> findByCompanyIdAndDeviceType(@Param("companyId") Long companyId, @Param("deviceType") DeviceType deviceType, Pageable pageable);

    /**
     * 根据企业ID和关键字搜索设备
     */
    @Query("SELECT d FROM Device d WHERE d.company.id = :companyId AND (d.deviceName LIKE %:keyword% OR d.deviceCode LIKE %:keyword% OR d.description LIKE %:keyword%)")
    Page<Device> searchByCompanyIdAndKeyword(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据设备编码和企业ID查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.deviceCode = :deviceCode AND d.company.id = :companyId")
    Optional<Device> findByDeviceCodeAndCompanyId(@Param("deviceCode") String deviceCode, @Param("companyId") Long companyId);

    /**
     * 检查设备编码在指定企业中是否存在
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Device d WHERE d.deviceCode = :deviceCode AND d.company.id = :companyId")
    boolean existsByDeviceCodeAndCompanyId(@Param("deviceCode") String deviceCode, @Param("companyId") Long companyId);

    /**
     * 根据企业ID和设备状态统计设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.company.id = :companyId AND d.status = :status")
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") DeviceStatus status);
}