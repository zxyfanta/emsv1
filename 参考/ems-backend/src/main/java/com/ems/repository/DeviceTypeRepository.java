package com.ems.repository;

import com.ems.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 设备类型数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {

    /**
     * 根据类型代码查找设备类型
     */
    Optional<DeviceType> findByTypeCode(String typeCode);

    /**
     * 检查类型代码是否存在
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * 查找所有启用的设备类型
     */
    List<DeviceType> findByEnabledTrue();

    /**
     * 查找所有禁用的设备类型
     */
    List<DeviceType> findByEnabledFalse();

    /**
     * 根据表名查找设备类型
     */
    Optional<DeviceType> findByTableName(String tableName);

    /**
     * 统计启用的设备类型数量
     */
    @Query("SELECT COUNT(dt) FROM DeviceType dt WHERE dt.enabled = true")
    long countEnabledDeviceTypes();

    /**
     * 根据启用状态统计设备类型数量
     */
    @Query("SELECT dt.enabled, COUNT(dt) FROM DeviceType dt GROUP BY dt.enabled")
    List<Object[]> countByEnabledStatus();

    /**
     * 搜索设备类型（按名称或描述）
     */
    @Query("SELECT dt FROM DeviceType dt WHERE " +
           "LOWER(dt.typeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(dt.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DeviceType> findByKeyword(@Param("keyword") String keyword);

    /**
     * 查找最近创建的设备类型
     */
    @Query("SELECT dt FROM DeviceType dt ORDER BY dt.createdAt DESC")
    List<DeviceType> findRecentlyCreated();
}