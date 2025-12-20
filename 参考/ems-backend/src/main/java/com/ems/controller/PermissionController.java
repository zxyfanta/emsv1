package com.ems.controller;

import com.ems.exception.BusinessException;
import com.ems.dto.common.ApiResponse;
import com.ems.entity.Permission;
import com.ems.entity.RolePermission;
import com.ems.entity.User;
import com.ems.exception.ErrorCode;
import com.ems.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 权限管理控制器
 * 提供权限管理的RESTful API接口
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "权限管理", description = "权限管理相关接口")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 获取权限详情
     */
    @GetMapping("/{permissionCode}")
    @Operation(summary = "获取权限详情", description = "根据权限编码获取权限详情")
    public ResponseEntity<ApiResponse<Permission>> getPermission(
            @Parameter(description = "权限编码", required = true)
            @PathVariable String permissionCode) {

        Optional<Permission> permission = permissionService.getPermission(permissionCode);
        return permission.map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                         .orElse(ResponseEntity.ok(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND, "权限不存在")));
    }

    /**
     * 获取用户的菜单权限
     */
    @GetMapping("/menu")
    @Operation(summary = "获取用户菜单权限", description = "获取当前用户的菜单权限列表")
    public ResponseEntity<ApiResponse<List<Permission>>> getUserMenuPermissions(
            @AuthenticationPrincipal User user) {

        List<Permission> permissions = permissionService.getUserMenuPermissions(user);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    /**
     * 获取用户的API权限
     */
    @GetMapping("/api")
    @Operation(summary = "获取用户API权限", description = "获取当前用户的API权限列表")
    public ResponseEntity<ApiResponse<Set<String>>> getUserApiPermissions(
            @AuthenticationPrincipal User user) {

        Set<String> permissions = permissionService.getUserApiPermissions(user);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    /**
     * 检查用户权限
     */
    @GetMapping("/check")
    @Operation(summary = "检查用户权限", description = "检查当前用户是否有指定权限")
    public ResponseEntity<ApiResponse<Boolean>> checkPermission(
            @Parameter(description = "权限编码", required = true)
            @RequestParam String permissionCode,
            @AuthenticationPrincipal User user) {

        boolean hasPermission = permissionService.hasPermission(user, permissionCode);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }

    /**
     * 获取权限树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取权限树", description = "获取完整的权限树结构")
    public ResponseEntity<ApiResponse<List<Permission>>> getPermissionTree() {
        List<Permission> permissionTree = permissionService.getPermissionTree();
        return ResponseEntity.ok(ApiResponse.success(permissionTree));
    }

    /**
     * 获取顶级权限
     */
    @GetMapping("/top-level")
    @Operation(summary = "获取顶级权限", description = "获取所有顶级权限（无父权限）")
    public ResponseEntity<ApiResponse<List<Permission>>> getTopLevelPermissions() {
        List<Permission> permissions = permissionService.getTopLevelPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    /**
     * 获取子权限
     */
    @GetMapping("/{parentId}/children")
    @Operation(summary = "获取子权限", description = "获取指定父权限的所有子权限")
    public ResponseEntity<ApiResponse<List<Permission>>> getChildPermissions(
            @Parameter(description = "父权限ID", required = true)
            @PathVariable Long parentId) {

        // 这里需要先获取父权限对象，简化实现
        Permission parentPermission = new Permission();
        parentPermission.setId(parentId);

        List<Permission> childPermissions = permissionService.getChildPermissions(parentPermission);
        return ResponseEntity.ok(ApiResponse.success(childPermissions));
    }

    /**
     * 分页查询权限
     */
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "分页查询权限", description = "分页查询权限列表")
    public ResponseEntity<ApiResponse<Page<Permission>>> getPermissions(
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段")
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @Parameter(description = "排序方向")
            @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "权限类型")
            @RequestParam(required = false) Permission.PermissionType permissionType,
            @Parameter(description = "资源类型")
            @RequestParam(required = false) Permission.ResourceType resourceType,
            @Parameter(description = "权限名称（模糊查询）")
            @RequestParam(required = false) String permissionName) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Permission> permissionPage = permissionService.getPermissions(
                pageable, permissionType, resourceType, permissionName);
        return ResponseEntity.ok(ApiResponse.success(permissionPage));
    }

    /**
     * 创建权限
     */
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "创建权限", description = "创建新的权限")
    public ResponseEntity<ApiResponse<Permission>> createPermission(
            @Valid @RequestBody Permission permission,
            @AuthenticationPrincipal User currentUser) {

        Permission createdPermission = permissionService.createPermission(permission);
        return ResponseEntity.ok(ApiResponse.success(createdPermission));
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "更新权限", description = "更新指定的权限")
    public ResponseEntity<ApiResponse<Permission>> updatePermission(
            @Parameter(description = "权限ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Permission permission,
            @AuthenticationPrincipal User currentUser) {

        Permission updatedPermission = permissionService.updatePermission(id, permission);
        return ResponseEntity.ok(ApiResponse.success(updatedPermission));
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "删除权限", description = "删除指定的权限")
    public ResponseEntity<ApiResponse<Void>> deletePermission(
            @Parameter(description = "权限ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 分配权限给角色
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "分配权限给角色", description = "将权限分配给指定角色")
    public ResponseEntity<ApiResponse<RolePermission>> assignPermissionToRole(
            @Parameter(description = "用户角色", required = true)
            @RequestParam User.UserRole role,
            @Parameter(description = "权限ID", required = true)
            @RequestParam Long permissionId,
            @Parameter(description = "权限范围")
            @RequestParam(defaultValue = "ALL") RolePermission.PermissionScope scope,
            @Parameter(description = "过期时间")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiredAt,
            @AuthenticationPrincipal User currentUser) {

        RolePermission rolePermission = permissionService.assignPermissionToRole(
                role, permissionId, scope, expiredAt);
        return ResponseEntity.ok(ApiResponse.success(rolePermission));
    }

    /**
     * 移除角色权限
     */
    @DeleteMapping("/revoke")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "移除角色权限", description = "移除角色的指定权限")
    public ResponseEntity<ApiResponse<Void>> removePermissionFromRole(
            @Parameter(description = "用户角色", required = true)
            @RequestParam User.UserRole role,
            @Parameter(description = "权限ID", required = true)
            @RequestParam Long permissionId,
            @AuthenticationPrincipal User currentUser) {

        permissionService.removePermissionFromRole(role, permissionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 批量更新权限启用状态
     */
    @PutMapping("/batch-enabled")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "批量更新启用状态", description = "批量更新权限的启用状态")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateEnabled(
            @Parameter(description = "权限ID列表", required = true)
            @RequestParam List<Long> ids,
            @Parameter(description = "启用状态", required = true)
            @RequestParam Boolean enabled,
            @AuthenticationPrincipal User currentUser) {

        int updatedCount = permissionService.batchUpdateEnabled(ids, enabled);
        return ResponseEntity.ok(ApiResponse.success(updatedCount));
    }

    /**
     * 获取权限统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "获取权限统计", description = "获取权限的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPermissionStatistics() {
        Map<String, Object> statistics = permissionService.getPermissionStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 清理过期权限关联
     */
    @PostMapping("/cleanup-expired")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "清理过期权限", description = "清理已过期的权限关联")
    public ResponseEntity<ApiResponse<Integer>> cleanupExpiredPermissions() {
        int cleanedCount = permissionService.cleanupExpiredPermissions();
        return ResponseEntity.ok(ApiResponse.success(cleanedCount));
    }

    /**
     * 验证权限编码
     */
    @PostMapping("/validate-code")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "验证权限编码", description = "验证权限编码是否可用")
    public ResponseEntity<ApiResponse<Boolean>> validatePermissionCode(
            @Parameter(description = "权限编码", required = true)
            @RequestParam String permissionCode,
            @Parameter(description = "权限ID（编辑时需要排除自己）")
            @RequestParam(required = false) Long excludeId) {

        // 这里应该调用权限服务验证编码，简化实现
        boolean isValid = permissionCode != null && permissionCode.matches("^[A-Z][A-Z0-9_:]*$");
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }
}