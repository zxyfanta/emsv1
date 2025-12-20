package com.ems.repository;

import com.ems.entity.RolePermission;
import com.ems.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色权限关联数据访问层
 *
 * @author EMS Team
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long>, JpaSpecificationExecutor<RolePermission> {

    /**
     * 根据角色查找权限关联
     *
     * @param role    用户角色
     * @param enabled 是否启用
     * @return 权限关联列表
     */
    List<RolePermission> findByRoleAndEnabled(User.UserRole role, Boolean enabled);

    /**
     * 根据权限查找角色关联
     *
     * @param permission 权限实体
     * @param enabled    是否启用
     * @return 角色权限关联列表
     */
    List<RolePermission> findByPermissionAndEnabled(com.ems.entity.Permission permission, Boolean enabled);

    /**
     * 根据角色和权限查找关联
     *
     * @param role       用户角色
     * @param permission 权限实体
     * @param enabled    是否启用
     * @return 角色权限关联
     */
    RolePermission findByRoleAndPermissionAndEnabled(User.UserRole role, com.ems.entity.Permission permission, Boolean enabled);

    /**
     * 根据角色ID和权限ID查找关联
     *
     * @param roleId       角色ID
     * @param permissionId 权限ID
     * @return 角色权限关联
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.permission.id = :permissionId")
    RolePermission findByRoleAndPermissionId(@Param("role") User.UserRole role, @Param("permissionId") Long permissionId);

    /**
     * 检查角色是否有指定权限
     *
     * @param role           用户角色
     * @param permissionCode 权限编码
     * @param enabled        是否启用
     * @return 是否有权限
     */
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp " +
           "JOIN rp.permission p " +
           "WHERE rp.role = :role AND p.permissionCode = :permissionCode " +
           "AND rp.enabled = :enabled AND (rp.expiredAt IS NULL OR rp.expiredAt > :currentTime)")
    boolean hasPermission(@Param("role") User.UserRole role,
                         @Param("permissionCode") String permissionCode,
                         @Param("enabled") Boolean enabled,
                         @Param("currentTime") LocalDateTime currentTime);

    /**
     * 获取角色的有效权限
     *
     * @param role 用户角色
     * @return 有效权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "WHERE rp.role = :role AND rp.enabled = true " +
           "AND (rp.expiredAt IS NULL OR rp.expiredAt > :currentTime) " +
           "ORDER BY rp.priority DESC")
    List<RolePermission> findValidPermissionsByRole(@Param("role") User.UserRole role,
                                                    @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找即将过期的权限关联
     *
     * @param beforeTime 时间阈值
     * @return 权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "WHERE rp.expiredAt IS NOT NULL AND rp.expiredAt <= :beforeTime AND rp.enabled = true")
    List<RolePermission> findExpiringPermissions(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查找已过期的权限关联
     *
     * @param currentTime 当前时间
     * @return 已过期的权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "WHERE rp.expiredAt IS NOT NULL AND rp.expiredAt <= :currentTime AND rp.enabled = true")
    List<RolePermission> findExpiredPermissions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 批量禁用过期的权限关联
     *
     * @param currentTime 当前时间
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE RolePermission rp SET rp.enabled = false " +
           "WHERE rp.expiredAt IS NOT NULL AND rp.expiredAt <= :currentTime")
    int disableExpiredPermissions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据权限范围查找关联
     *
     * @param scope  权限范围
     * @param enabled 是否启用
     * @return 权限关联列表
     */
    List<RolePermission> findByScopeAndEnabled(RolePermission.PermissionScope scope, Boolean enabled);

    /**
     * 统计各角色的权限数量
     *
     * @return 统计结果
     */
    @Query("SELECT rp.role, COUNT(rp) FROM RolePermission rp WHERE rp.enabled = true " +
           "GROUP BY rp.role")
    List<Object[]> countPermissionsByRole();

    /**
     * 统计各权限范围的权限数量
     *
     * @return 统计结果
     */
    @Query("SELECT rp.scope, COUNT(rp) FROM RolePermission rp WHERE rp.enabled = true " +
           "GROUP BY rp.scope")
    List<Object[]> countPermissionsByScope();

    /**
     * 根据优先级范围查找权限关联
     *
     * @param minPriority 最小优先级
     * @param maxPriority 最大优先级
     * @param enabled     是否启用
     * @return 权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "WHERE rp.priority BETWEEN :minPriority AND :maxPriority AND rp.enabled = :enabled " +
           "ORDER BY rp.priority DESC")
    List<RolePermission> findByPriorityRange(@Param("minPriority") Integer minPriority,
                                            @Param("maxPriority") Integer maxPriority,
                                            @Param("enabled") Boolean enabled);

    /**
     * 批量删除角色的权限关联
     *
     * @param role 用户角色
     * @return 删除行数
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role")
    int deleteByRole(@Param("role") User.UserRole role);

    /**
     * 批量删除权限的角色关联
     *
     * @param permission 权限实体
     * @return 删除行数
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.permission = :permission")
    int deleteByPermission(@Param("permission") com.ems.entity.Permission permission);

    /**
     * 批量更新权限关联的启用状态
     *
     * @param ids     权限关联ID列表
     * @param enabled 启用状态
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE RolePermission rp SET rp.enabled = :enabled WHERE rp.id IN :ids")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);

    /**
     * 查找指定时间后过期的权限关联
     *
     * @param currentTime 当前时间
     * @param days        天数
     * @return 权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "WHERE rp.expiredAt IS NOT NULL " +
           "AND rp.expiredAt BETWEEN :currentTime AND :expireTime " +
           "AND rp.enabled = true")
    List<RolePermission> findPermissionsExpiringInDays(@Param("currentTime") LocalDateTime currentTime,
                                                      @Param("expireTime") LocalDateTime expireTime);

    /**
     * 根据条件查找权限关联
     *
     * @param role           用户角色
     * @param permissionType 权限类型
     * @param enabled        是否启用
     * @return 权限关联列表
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "JOIN rp.permission p " +
           "WHERE rp.role = :role AND p.permissionType = :permissionType AND rp.enabled = :enabled")
    List<RolePermission> findByRoleAndPermissionType(@Param("role") User.UserRole role,
                                                    @Param("permissionType") com.ems.entity.Permission.PermissionType permissionType,
                                                    @Param("enabled") Boolean enabled);
}