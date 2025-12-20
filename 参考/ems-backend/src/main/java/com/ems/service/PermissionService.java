package com.ems.service;

import com.ems.exception.BusinessException;
import com.ems.exception.ValidationException;
import com.ems.entity.Permission;
import com.ems.entity.RolePermission;
import com.ems.entity.User;
import com.ems.exception.ErrorCode;
import com.ems.repository.PermissionRepository;
import com.ems.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限服务层
 * 提供权限管理的核心功能
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * 根据权限编码获取权限
     *
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    @Cacheable(value = "permission", key = "#permissionCode")
    public Optional<Permission> getPermission(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return Optional.empty();
        }
        return permissionRepository.findByPermissionCodeAndEnabledTrue(permissionCode);
    }

    /**
     * 获取用户的菜单权限
     *
     * @param user 用户
     * @return 菜单权限列表
     */
    @Cacheable(value = "userMenuPermissions", key = "#user.id")
    public List<Permission> getUserMenuPermissions(User user) {
        if (user == null) {
            return List.of();
        }

        return permissionRepository.findMenuPermissionsByRole(user.getRole());
    }

    /**
     * 获取用户的API权限
     *
     * @param user 用户
     * @return API权限列表
     */
    @Cacheable(value = "userApiPermissions", key = "#user.id")
    public Set<String> getUserApiPermissions(User user) {
        if (user == null) {
            return Set.of();
        }

        List<Permission> permissions = permissionRepository.findApiPermissionsByRole(user.getRole());
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param user           用户
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    public boolean hasPermission(User user, String permissionCode) {
        if (user == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        // 平台管理员拥有所有权限
        if (User.UserRole.PLATFORM_ADMIN.equals(user.getRole())) {
            return true;
        }

        return rolePermissionRepository.hasPermission(user.getRole(), permissionCode, true, LocalDateTime.now());
    }

    /**
     * 检查用户是否有任一权限
     *
     * @param user            用户
     * @param permissionCodes 权限编码列表
     * @return 是否有权限
     */
    public boolean hasAnyPermission(User user, String... permissionCodes) {
        if (user == null || permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }

        // 平台管理员拥有所有权限
        if (User.UserRole.PLATFORM_ADMIN.equals(user.getRole())) {
            return true;
        }

        for (String permissionCode : permissionCodes) {
            if (hasPermission(user, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否有所有权限
     *
     * @param user            用户
     * @param permissionCodes 权限编码列表
     * @return 是否有权限
     */
    public boolean hasAllPermissions(User user, String... permissionCodes) {
        if (user == null || permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }

        // 平台管理员拥有所有权限
        if (User.UserRole.PLATFORM_ADMIN.equals(user.getRole())) {
            return true;
        }

        for (String permissionCode : permissionCodes) {
            if (!hasPermission(user, permissionCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 分页查询权限
     *
     * @param pageable       分页参数
     * @param permissionType 权限类型（可选）
     * @param resourceType   资源类型（可选）
     * @param permissionName 权限名称（可选，模糊查询）
     * @return 权限分页
     */
    public Page<Permission> getPermissions(Pageable pageable,
                                          Permission.PermissionType permissionType,
                                          Permission.ResourceType resourceType,
                                          String permissionName) {
        // 根据条件查询
        if (permissionType != null) {
            return permissionRepository.findByPermissionTypeAndEnabled(permissionType, true, pageable);
        } else if (resourceType != null) {
            return permissionRepository.findByResourceTypeAndEnabled(resourceType, true, pageable);
        } else if (StringUtils.hasText(permissionName)) {
            return permissionRepository.findByPermissionNameContainingIgnoreCaseAndEnabled(permissionName, true, pageable);
        } else {
            return permissionRepository.findAll(pageable);
        }
    }

    /**
     * 获取权限树
     *
     * @return 权限树
     */
    @Cacheable(value = "permissionTree")
    public List<Permission> getPermissionTree() {
        return permissionRepository.findPermissionTree(true);
    }

    /**
     * 获取顶级权限
     *
     * @return 顶级权限列表
     */
    public List<Permission> getTopLevelPermissions() {
        return permissionRepository.findByParentPermissionIsNullAndEnabledTrueOrderBySortOrderAsc(true);
    }

    /**
     * 获取子权限
     *
     * @param parentPermission 父权限
     * @return 子权限列表
     */
    public List<Permission> getChildPermissions(Permission parentPermission) {
        return permissionRepository.findByParentPermissionAndEnabledTrueOrderBySortOrder(parentPermission);
    }

    /**
     * 创建权限
     *
     * @param permission 权限信息
     * @return 创建的权限
     */
    @Transactional
    @CacheEvict(value = {"permission", "permissionTree", "userMenuPermissions", "userApiPermissions"}, allEntries = true)
    public Permission createPermission(Permission permission) {
        validatePermission(permission, true);

        // 检查权限编码是否已存在
        if (permissionRepository.existsByPermissionCode(permission.getPermissionCode())) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE,
                    "权限编码已存在: " + permission.getPermissionCode());
        }

        Permission savedPermission = permissionRepository.save(permission);
        log.info("创建权限成功: code={}, name={}",
                permission.getPermissionCode(), permission.getPermissionName());

        return savedPermission;
    }

    /**
     * 更新权限
     *
     * @param id         权限ID
     * @param newPermission 新权限信息
     * @return 更新后的权限
     */
    @Transactional
    @CacheEvict(value = {"permission", "permissionTree", "userMenuPermissions", "userApiPermissions"}, allEntries = true)
    public Permission updatePermission(Long id, Permission newPermission) {
        Optional<Permission> permissionOpt = permissionRepository.findById(id);
        if (permissionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限不存在");
        }

        Permission permission = permissionOpt.get();

        // 系统权限不允许修改权限编码和权限类型
        if (permission.isSystemPermission()) {
            if (!permission.getPermissionCode().equals(newPermission.getPermissionCode())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "系统权限不允许修改权限编码");
            }
            if (!permission.getPermissionType().equals(newPermission.getPermissionType())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "系统权限不允许修改权限类型");
            }
        }

        // 验证权限
        validatePermission(newPermission, false);

        // 更新字段
        permission.setPermissionName(newPermission.getPermissionName());
        permission.setDescription(newPermission.getDescription());
        permission.setResourceType(newPermission.getResourceType());
        permission.setResourceId(newPermission.getResourceId());
        permission.setActionType(newPermission.getActionType());
        permission.setPermissionLevel(newPermission.getPermissionLevel());
        permission.setParentPermission(newPermission.getParentPermission());
        permission.setSortOrder(newPermission.getSortOrder());
        permission.setIcon(newPermission.getIcon());
        permission.setPath(newPermission.getPath());
        permission.setEnabled(newPermission.getEnabled());

        Permission savedPermission = permissionRepository.save(permission);
        log.info("更新权限成功: code={}, name={}",
                permission.getPermissionCode(), permission.getPermissionName());

        return savedPermission;
    }

    /**
     * 删除权限
     *
     * @param id 权限ID
     */
    @Transactional
    @CacheEvict(value = {"permission", "permissionTree", "userMenuPermissions", "userApiPermissions"}, allEntries = true)
    public void deletePermission(Long id) {
        Optional<Permission> permissionOpt = permissionRepository.findById(id);
        if (permissionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限不存在");
        }

        Permission permission = permissionOpt.get();

        // 系统权限不允许删除
        if (permission.isSystemPermission()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "系统权限不允许删除");
        }

        // 检查是否有子权限
        List<Permission> childPermissions = getChildPermissions(permission);
        if (!childPermissions.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "存在子权限，无法删除");
        }

        // 检查是否有角色在使用此权限
        List<RolePermission> rolePermissions = rolePermissionRepository.findByPermissionAndEnabled(permission, true);
        if (!rolePermissions.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "权限正在被使用，无法删除");
        }

        permissionRepository.delete(permission);
        log.info("删除权限成功: code={}, name={}",
                permission.getPermissionCode(), permission.getPermissionName());
    }

    /**
     * 分配权限给角色
     *
     * @param role       用户角色
     * @param permissionId 权限ID
     * @param scope      权限范围
     * @param expiredAt  过期时间
     * @return 角色权限关联
     */
    @Transactional
    @CacheEvict(value = {"userMenuPermissions", "userApiPermissions"}, allEntries = true)
    public RolePermission assignPermissionToRole(User.UserRole role, Long permissionId,
                                                RolePermission.PermissionScope scope,
                                                LocalDateTime expiredAt) {
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限不存在");
        }

        Permission permission = permissionOpt.get();
        if (!permission.isEnabled()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "权限已禁用");
        }

        // 检查是否已存在关联
        RolePermission existingRolePermission = rolePermissionRepository
                .findByRoleAndPermissionAndEnabled(role, permission, true);
        if (existingRolePermission != null) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "权限已分配给该角色");
        }

        RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .enabled(true)
                .scope(scope)
                .expiredAt(expiredAt)
                .priority(1)
                .build();

        RolePermission savedRolePermission = rolePermissionRepository.save(rolePermission);
        log.info("分配权限给角色成功: role={}, permissionCode={}, scope={}",
                role.name(), permission.getPermissionCode(), scope);

        return savedRolePermission;
    }

    /**
     * 移除角色的权限
     *
     * @param role       用户角色
     * @param permissionId 权限ID
     */
    @Transactional
    @CacheEvict(value = {"userMenuPermissions", "userApiPermissions"}, allEntries = true)
    public void removePermissionFromRole(User.UserRole role, Long permissionId) {
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限不存在");
        }

        Permission permission = permissionOpt.get();
        RolePermission rolePermission = rolePermissionRepository
                .findByRoleAndPermissionAndEnabled(role, permission, true);
        if (rolePermission == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色权限关联不存在");
        }

        rolePermission.setEnabled(false);
        rolePermissionRepository.save(rolePermission);
        log.info("移除角色权限成功: role={}, permissionCode={}",
                role.name(), permission.getPermissionCode());
    }

    /**
     * 批量更新权限启用状态
     *
     * @param ids     权限ID列表
     * @param enabled 启用状态
     * @return 更新行数
     */
    @Transactional
    @CacheEvict(value = {"permission", "permissionTree", "userMenuPermissions", "userApiPermissions"}, allEntries = true)
    public int batchUpdateEnabled(List<Long> ids, Boolean enabled) {
        int updatedCount = permissionRepository.batchUpdateEnabled(ids, enabled);
        log.info("批量更新权限启用状态成功: count={}, enabled={}", updatedCount, enabled);
        return updatedCount;
    }

    /**
     * 获取权限统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getPermissionStatistics() {
        List<Object[]> typeResults = permissionRepository.countByPermissionType();
        List<Object[]> resourceResults = permissionRepository.countByResourceType();

        Map<String, Object> statistics = Map.of(
                "byType", typeResults.stream()
                        .collect(Collectors.toMap(
                                result -> ((Permission.PermissionType) result[0]).getDescription(),
                                result -> result[1]
                        )),
                "byResource", resourceResults.stream()
                        .collect(Collectors.toMap(
                                result -> ((Permission.ResourceType) result[0]).getDescription(),
                                result -> result[1]
                        ))
        );

        return statistics;
    }

    /**
     * 清理过期权限关联
     *
     * @return 清理数量
     */
    @Transactional
    public int cleanupExpiredPermissions() {
        int disabledCount = rolePermissionRepository.disableExpiredPermissions(LocalDateTime.now());
        log.info("清理过期权限关联完成: disabledCount={}", disabledCount);
        return disabledCount;
    }

    /**
     * 验证权限信息
     *
     * @param permission 权限信息
     * @param isNew      是否为新建
     */
    private void validatePermission(Permission permission, boolean isNew) {
        if (permission == null) {
            throw new ValidationException("权限信息不能为空");
        }

        // 验证必填字段
        if (!StringUtils.hasText(permission.getPermissionCode())) {
            throw new ValidationException("权限编码不能为空");
        }

        if (!StringUtils.hasText(permission.getPermissionName())) {
            throw new ValidationException("权限名称不能为空");
        }

        if (permission.getPermissionType() == null) {
            throw new ValidationException("权限类型不能为空");
        }

        if (permission.getResourceType() == null) {
            throw new ValidationException("资源类型不能为空");
        }

        if (permission.getPermissionLevel() == null) {
            throw new ValidationException("权限级别不能为空");
        }

        // 验证权限编码格式
        if (!permission.getPermissionCode().matches("^[A-Z][A-Z0-9_:]*$")) {
            throw new ValidationException("权限编码格式不正确，应为大写字母、数字、下划线和冒号");
        }

        // 验证父子关系
        if (permission.getParentPermission() != null) {
            if (permission.getParentPermission().getId().equals(permission.getId())) {
                throw new ValidationException("父权限不能是自己");
            }
        }
    }
}