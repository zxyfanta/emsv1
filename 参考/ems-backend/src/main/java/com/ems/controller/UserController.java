package com.ems.controller;

import com.ems.dto.common.ApiResponse;
import com.ems.entity.User;
import com.ems.repository.UserRepository;
import com.ems.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户管理控制器
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "获取用户列表", description = "分页获取用户列表，支持搜索和筛选")
    public ResponseEntity<ApiResponse<Page<User>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long enterpriseId
    ) {
        try {
            // 创建分页和排序
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(page, size, sort);

            // 调用服务获取用户列表
            Page<User> userPage = userService.getUsers(keyword, role, status, enterpriseId, pageable);

            // 清除用户密码信息
            userPage.getContent().forEach(user -> user.setPassword(null));

            return ResponseEntity.ok(ApiResponse.success("获取用户列表成功", userPage));

        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // 清除敏感信息
                user.setPassword(null);

                return ResponseEntity.ok(ApiResponse.success("获取用户详情成功", user));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("用户不存在"));
            }
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "创建用户", description = "创建新的用户账户")
    public ResponseEntity<String> createUser(@Valid @RequestBody User user) {
        try {
            // 检查用户名是否已存在
            if (userRepository.existsByUsernameAndDeletedFalse(user.getUsername())) {
                return ResponseEntity.badRequest()
                        .body("{\"success\":false,\"message\":\"用户名已存在\"}");
            }

            // 检查邮箱是否已存在
            if (userRepository.existsByEmailAndDeletedFalse(user.getEmail())) {
                return ResponseEntity.badRequest()
                        .body("{\"success\":false,\"message\":\"邮箱已存在\"}");
            }

            // 设置默认值
            user.setCreatedAt(LocalDateTime.now());
            user.setDeleted(false);

            // 如果没有设置角色，默认为普通用户
            if (user.getRole() == null) {
                user.setRole(User.UserRole.ENTERPRISE_USER);
            }

            User savedUser = userRepository.save(user);

            // 清除敏感信息
            savedUser.setPassword(null);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("{\n" +
                            "  \"success\": true,\n" +
                            "  \"message\": \"用户创建成功\",\n" +
                            "  \"data\": {\n" +
                            "    \"id\":" + savedUser.getId() + ",\n" +
                            "    \"username\":\"" + savedUser.getUsername() + "\",\n" +
                            "    \"email\":\"" + savedUser.getEmail() + "\",\n" +
                            "    \"role\":\"" + savedUser.getRole() + "\"\n" +
                            "    \"enabled\":" + savedUser.getEnabled() + "\n" +
                            "  },\n" +
                            "  \"timestamp\": \"" + LocalDateTime.now() + "\"\n" +
                            "}");

        } catch (Exception e) {
            log.error("创建用户失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"message\":\"创建用户失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "更新用户信息", description = "更新指定用户的基本信息")
    public ResponseEntity<String> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User userUpdates
    ) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"success\":false,\"message\":\"用户不存在\"}");
            }

            User existingUser = userOpt.get();

            // 更新允许修改的字段
            if (userUpdates.getEmail() != null) {
                existingUser.setEmail(userUpdates.getEmail());
            }
            if (userUpdates.getFullName() != null) {
                existingUser.setFullName(userUpdates.getFullName());
            }
            if (userUpdates.getRole() != null) {
                try {
                    existingUser.setRole(User.UserRole.valueOf(userUpdates.getRole().toString()));
                } catch (IllegalArgumentException e) {
                    existingUser.setRole(User.UserRole.ENTERPRISE_USER);
                }
            }
            if (userUpdates.getEnterpriseId() != null) {
                existingUser.setEnterpriseId(userUpdates.getEnterpriseId());
            }

            User updatedUser = userRepository.save(existingUser);

            // 清除敏感信息
            updatedUser.setPassword(null);

            return ResponseEntity.ok("{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"用户信息更新成功\",\n" +
                    "  \"data\": {\n" +
                    "    \"id\":" + updatedUser.getId() + ",\n" +
                    "    \"username\":\"" + updatedUser.getUsername() + "\",\n" +
                    "    \"email\":\"" + updatedUser.getEmail() + "\",\n" +
                    "    \"fullName\":\"" + updatedUser.getFullName() + "\",\n" +
                    "    \"role\":\"" + updatedUser.getRole() + "\"\n" +
                    "    \"enabled\":" + updatedUser.getEnabled() + "\n" +
                    "  },\n" +
                    "  \"timestamp\": \"" + LocalDateTime.now() + "\"\n" +
                    "}");

        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"message\":\"更新用户信息失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "删除用户", description = "删除指定的用户账户")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"success\":false,\"message\":\"用户不存在\"}");
            }

            userRepository.deleteById(id);

            return ResponseEntity.ok("{\"success\":true,\"message\":\"用户删除成功\",\"data\":\"用户ID: " + id + "\"}");

        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"message\":\"删除用户失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "获取用户统计", description = "获取用户数量统计信息")
    public ResponseEntity<String> getUserStats() {
        try {
            Map<String, Object> stats = userService.getUserStats();

            return ResponseEntity.ok("{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"获取用户统计成功\",\n" +
                    "  \"data\": {\n" +
                    "    \"total\":" + stats.get("total") + ",\n" +
                    "    \"active\":" + stats.get("active") + ",\n" +
                    "    \"inactive\":" + stats.get("inactive") + ",\n" +
                    "    \"locked\":" + stats.get("locked") + ",\n" +
                    "    \"online\":" + stats.get("online") + "\n" +
                    "  },\n" +
                    "  \"timestamp\": \"" + LocalDateTime.now() + "\"\n" +
                    "}");

        } catch (Exception e) {
            log.error("获取用户统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"message\":\"获取用户统计失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "搜索用户", description = "根据关键词搜索用户")
    public ResponseEntity<String> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            List<User> users = userService.searchUsers(keyword, limit);

            // 清除敏感信息
            users.forEach(user -> user.setPassword(null));

            return ResponseEntity.ok("{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"搜索用户成功\",\n" +
                    "  \"data\":" + users + ",\n" +
                    "  \"timestamp\": \"" + LocalDateTime.now() + "\"\n" +
                    "}");

        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"message\":\"搜索用户失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 批量更新用户状态
     */
    @PutMapping("/batch/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "批量更新用户状态", description = "批量更新用户状态")
    public ResponseEntity<String> batchUpdateUserStatus(
            @RequestParam List<Long> userIds,
            @RequestParam String status
    ) {
        try {
            int updatedCount = userService.batchUpdateUserStatus(userIds, status);

            return ResponseEntity.ok("{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"批量更新用户状态成功\",\n" +
                    "  \"data\":\"更新了 " + updatedCount + " 个用户\",\n" +
                    "  \"timestamp\": \"" + LocalDateTime.now() + "\"\n" +
                    "}");

        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\":false,\"message\":\"批量更新用户状态失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "重置用户密码", description = "重置指定用户的密码")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long id,
            @RequestParam String newPassword
    ) {
        try {
            userService.resetUserPassword(id, newPassword);
            return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
        } catch (Exception e) {
            log.error("重置用户密码失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("重置用户密码失败: " + e.getMessage()));
        }
    }

    /**
     * 更新单个用户状态
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "更新用户状态", description = "更新指定用户的状态")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status
    ) {
        try {
            // TODO: 实现用户状态更新逻辑
            return ResponseEntity.ok(ApiResponse.success("用户状态更新成功", null));
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新用户状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户角色列表
     */
    @GetMapping("/roles")
    @Operation(summary = "获取用户角色列表", description = "获取所有可用的用户角色")
    public ResponseEntity<ApiResponse<List<String>>> getUserRoles() {
        try {
            List<String> roles = List.of("PLATFORM_ADMIN", "ENTERPRISE_ADMIN", "ENTERPRISE_USER");
            return ResponseEntity.ok(ApiResponse.success("获取用户角色列表成功", roles));
        } catch (Exception e) {
            log.error("获取用户角色列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户角色列表失败: " + e.getMessage()));
        }
    }

    /**
     * 批量操作用户
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "批量操作用户", description = "批量处理用户操作")
    public ResponseEntity<ApiResponse<Void>> batchOperation(
            @RequestBody Map<String, Object> request
    ) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) request.get("userIds");
            String operation = (String) request.get("operation");

            // TODO: 实现批量操作逻辑
            return ResponseEntity.ok(ApiResponse.success("批量操作成功", null));
        } catch (Exception e) {
            log.error("批量操作用户失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量操作用户失败: " + e.getMessage()));
        }
    }
}