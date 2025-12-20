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

import java.util.List;
import java.util.Optional;

/**
 * 设备分组数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long> {

    /**
     * 查找启用状态的分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enabled = true")
    List<DeviceGroup> findEnabledGroups();

    /**
     * 根据企业查找启用的分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enabled = true AND dg.enterprise = :enterprise")
    List<DeviceGroup> findEnabledGroupsByEnterprise(@Param("enterprise") Enterprise enterprise);

    /**
     * 根据企业查找启用的分组（分页）
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enterprise = :enterprise")
    Page<DeviceGroup> findGroupsByEnterprise(@Param("enterprise") Enterprise enterprise, Pageable pageable);

    /**
     * 根据名称和分组类型查找
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.name = :name AND dg.groupType = :groupType")
    Optional<DeviceGroup> findByNameAndType(@Param("name") String name, @Param("groupType") DeviceGroup.GroupType groupType);

    /**
     * 根据企业查找根分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.parentGroup IS NULL AND dg.enterprise = :enterprise")
    List<DeviceGroup> findRootGroupsByEnterprise(@Param("enterprise") Enterprise enterprise);

    /**
     * 根据父分组查找子分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.parentGroup = :parentGroup")
    List<DeviceGroup> findByParentGroup(@Param("parentGroup") DeviceGroup parentGroup);

    /**
     * 查找包含指定设备的分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.id IN " +
           "(SELECT dgm.deviceGroup.id FROM DeviceGroupMapping dgm WHERE dgm.device = :device AND dgm.enabled = true)")
    List<DeviceGroup> findGroupsContainingDevice(@Param("device") Device device);

    /**
     * 查找指定分组及其所有子分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND (dg = :group OR dg.parentGroup IN :subGroups)")
    List<DeviceGroup> findGroupAndChildren(@Param("group") DeviceGroup group, @Param("subGroups") List<DeviceGroup> subGroups);

    /**
     * 统计企业下的分组数量
     */
    @Query("SELECT COUNT(dg) FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enterprise = :enterprise")
    long countByEnterprise(@Param("enterprise") Enterprise enterprise);

    /**
     * 检查名称是否已存在
     */
    @Query("SELECT COUNT(dg) > 0 FROM DeviceGroup dg WHERE dg.deleted = false AND dg.name = :name AND dg.enterprise = :enterprise AND dg.id != :excludeId")
    boolean existsByNameAndEnterprise(@Param("name") String name, @Param("enterprise") Enterprise enterprise, @Param("excludeId") Long excludeId);

    /**
     * 检查名称是否已存在（新建时）
     */
    @Query("SELECT COUNT(dg) > 0 FROM DeviceGroup dg WHERE dg.deleted = false AND dg.name = :name AND dg.enterprise = :enterprise")
    boolean existsByNameAndEnterprise(@Param("name") String name, @Param("enterprise") Enterprise enterprise);

    /**
     * 按分组类型统计
     */
    @Query("SELECT dg.groupType, COUNT(dg) FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enterprise = :enterprise GROUP BY dg.groupType")
    List<Object[]> countByGroupType(@Param("enterprise") Enterprise enterprise);

    /**
     * 搜索分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enterprise = :enterprise AND " +
           "(LOWER(dg.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(dg.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DeviceGroup> searchGroups(@Param("enterprise") Enterprise enterprise, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找所有系统分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.groupType = 'SYSTEM'")
    List<DeviceGroup> findSystemGroups();

    /**
     * 获取分组的完整层级结构
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enterprise = :enterprise ORDER BY dg.sortOrder ASC, dg.name ASC")
    List<DeviceGroup> findAllByEnterpriseOrdered(@Param("enterprise") Enterprise enterprise);

    /**
     * 检查分组是否可以删除（没有子分组且没有设备）
     */
    @Query("SELECT CASE WHEN COUNT(dg) > 0 THEN true ELSE false END FROM DeviceGroup dg WHERE dg.deleted = false AND dg.parentGroup = :group")
    boolean hasChildGroups(@Param("group") DeviceGroup group);

    @Query("SELECT CASE WHEN COUNT(dgm) > 0 THEN true ELSE false END FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup = :group AND dgm.enabled = true")
    boolean hasDevices(@Param("group") DeviceGroup group);

    /**
     * 根据分组类型和企业查找分组
     */
    @Query("SELECT dg FROM DeviceGroup dg WHERE dg.deleted = false AND dg.enterprise = :enterprise AND dg.groupType = :groupType")
    List<DeviceGroup> findByGroupTypeAndEnterprise(@Param("groupType") DeviceGroup.GroupType groupType, @Param("enterprise") Enterprise enterprise);

    /**
     * 查找不在指定分组的设备
     */
    @Query("SELECT d FROM Device d WHERE d.deleted = false AND d.enterprise = :enterprise AND " +
           "d.id NOT IN (SELECT dgm.device.id FROM DeviceGroupMapping dgm WHERE dgm.deviceGroup.id = :groupId AND dgm.enabled = true)")
    List<Device> findUnassignedDevices(@Param("enterprise") Enterprise enterprise, @Param("groupId") Long groupId);
}