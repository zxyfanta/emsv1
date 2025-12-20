package com.ems.repository;

import com.ems.entity.Permission;
import com.ems.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限数据访问层
 *
 * @author EMS Team
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限编码查找权限
     *
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * 根据权限编码查找权限（仅启用状态）
     *
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    Optional<Permission> findByPermissionCodeAndEnabledTrue(String permissionCode);

    /**
     * 检查权限编码是否存在
     *
     * @param permissionCode 权限编码
     * @return 是否存在
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * 根据权限类型查找权限
     *
     * @param permissionType 权限类型
     * @param enabled        是否启用
     * @param pageable       分页参数
     * @return 权限列表
     */
    Page<Permission> findByPermissionTypeAndEnabled(Permission.PermissionType permissionType, Boolean enabled, Pageable pageable);

    /**
     * 根据资源类型查找权限
     *
     * @param resourceType 资源类型
     * @param enabled      是否启用
     * @param pageable     分页参数
     * @return 权限列表
     */
    Page<Permission> findByResourceTypeAndEnabled(Permission.ResourceType resourceType, Boolean enabled, Pageable pageable);

    /**
     * 根据父权限查找子权限
     *
     * @param parentPermission 父权限
     * @param enabled          是否启用
     * @return 子权限列表
     */
    List<Permission> findByParentPermissionAndEnabledTrueOrderBySortOrder(Permission parentPermission);

    /**
     * 查找顶级权限（无父权限）
     *
     * @param enabled 是否启用
     * @return 权限列表
     */
    List<Permission> findByParentPermissionIsNullAndEnabledTrueOrderBySortOrderAsc(Boolean enabled);

    /**
     * 根据权限名称模糊查询
     *
     * @param permissionName 权限名称
     * @param enabled        是否启用
     * @param pageable       分页参数
     * @return 权限列表
     */
    Page<Permission> findByPermissionNameContainingIgnoreCaseAndEnabled(String permissionName, Boolean enabled, Pageable pageable);

    /**
     * 根据权限级别查找权限
     *
     * @param permissionLevel 权限级别
     * @param enabled         是否启用
     * @return 权限列表
     */
    List<Permission> findByPermissionLevelAndEnabledTrueOrderBySortOrderAsc(Permission.PermissionLevel permissionLevel);

    /**
     * 查找菜单权限
     *
     * @param enabled 是否启用
     * @return 菜单权限列表
     */
    List<Permission> findByPermissionTypeAndEnabledTrueOrderBySortOrderAsc(Permission.PermissionType permissionType);

    /**
     * 查找系统权限
     *
     * @param enabled 是否启用
     * @return 系统权限列表
     */
    List<Permission> findByIsSystemTrueAndEnabled(Boolean enabled);

    /**
     * 根据资源类型和操作类型查找权限
     *
     * @param resourceType 资源类型
     * @param actionType   操作类型
     * @param enabled      是否启用
     * @return 权限列表
     */
    List<Permission> findByResourceTypeAndActionTypeAndEnabledTrue(Permission.ResourceType resourceType, Permission.ActionType actionType);

    /**
     * 统计各类型的权限数量
     *
     * @return 统计结果
     */
    @Query("SELECT p.permissionType, COUNT(p) FROM Permission p WHERE p.enabled = true GROUP BY p.permissionType")
    List<Object[]> countByPermissionType();

    /**
     * 统计各资源类型的权限数量
     *
     * @return 统计结果
     */
    @Query("SELECT p.resourceType, COUNT(p) FROM Permission p WHERE p.enabled = true GROUP BY p.resourceType")
    List<Object[]> countByResourceType();

    /**
     * 查找权限树
     *
     * @param enabled 是否启用
     * @return 权限树
     */
    @Query("SELECT p FROM Permission p WHERE p.enabled = :enabled ORDER BY p.parentPermission.id ASC NULLS FIRST, p.sortOrder ASC")
    List<Permission> findPermissionTree(@Param("enabled") Boolean enabled);

    /**
     * 根据权限ID列表查找权限
     *
     * @param ids 权限ID列表
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.id IN :ids AND p.enabled = true ORDER BY p.sortOrder ASC")
    List<Permission> findByIdInAndEnabledTrue(@Param("ids") List<Long> ids);

    /**
     * 查找用户的所有权限
     *
     * @param role 用户角色
     * @return 权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permission.id " +
           "WHERE rp.role = :role AND rp.enabled = true AND p.enabled = true " +
           "AND (rp.expiredAt IS NULL OR rp.expiredAt > CURRENT_TIMESTAMP)")
    List<Permission> findPermissionsByRole(@Param("role") User.UserRole role);

    /**
     * 查找用户的菜单权限
     *
     * @param role 用户角色
     * @return 菜单权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permission.id " +
           "WHERE rp.role = :role AND rp.enabled = true AND p.enabled = true " +
           "AND p.permissionType = 'MENU' " +
           "AND (rp.expiredAt IS NULL OR rp.expiredAt > CURRENT_TIMESTAMP) " +
           "ORDER BY p.sortOrder ASC")
    List<Permission> findMenuPermissionsByRole(@Param("role") User.UserRole role);

    /**
     * 查找用户的API权限
     *
     * @param role 用户角色
     * @return API权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permission.id " +
           "WHERE rp.role = :role AND rp.enabled = true AND p.enabled = true " +
           "AND p.permissionType = 'API' " +
           "AND (rp.expiredAt IS NULL OR rp.expiredAt > CURRENT_TIMESTAMP)")
    List<Permission> findApiPermissionsByRole(@Param("role") User.UserRole role);

    /**
     * 检查用户是否有指定权限
     *
     * @param role          用户角色
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permission.id " +
           "WHERE rp.role = :role AND p.permissionCode = :permissionCode " +
           "AND rp.enabled = true AND p.enabled = true " +
           "AND (rp.expiredAt IS NULL OR rp.expiredAt > CURRENT_TIMESTAMP)")
    boolean hasPermission(@Param("role") User.UserRole role, @Param("permissionCode") String permissionCode);

    /**
     * 批量更新权限启用状态
     *
     * @param ids     权限ID列表
     * @param enabled 启用状态
     * @return 更新行数
     */
    @Query("UPDATE Permission p SET p.enabled = :enabled WHERE p.id IN :ids")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
}