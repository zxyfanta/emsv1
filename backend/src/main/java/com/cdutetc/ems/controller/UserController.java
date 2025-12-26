package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.request.UserCreateRequest;
import com.cdutetc.ems.dto.response.UserResponse;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.enums.UserStatus;
import com.cdutetc.ems.security.JwtUtil;
import com.cdutetc.ems.service.UserService;
import com.cdutetc.ems.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 从请求中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) return null;
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建用户", description = "创建新的用户")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        try {
            // 检查用户名是否已存在
            if (userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("用户名已存在"));
            }

            // 检查邮箱是否已存在
            if (request.getEmail() != null && userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("邮箱已被使用"));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setStatus(UserStatus.ACTIVE);
            // 角色和企业需要通过UserService设置，因为它们涉及关联关系

            User createdUser = userService.createUser(user);
            UserResponse response = UserResponse.fromUser(createdUser);

            log.info("User created successfully: {}", createdUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(response));

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建用户失败"));
        }
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            User user = userService.findByIdWithCompany(id);

            // 验证权限：管理员可以查看所有用户，普通用户只能查看自己
            String token = extractTokenFromRequest(request);
            if (token != null) {
                String role = jwtUtil.getRoleFromToken(token);
                Long currentUserId = jwtUtil.getUserIdFromToken(token);

                if (!"ADMIN".equals(role) && !currentUserId.equals(id)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.forbidden("没有权限访问此用户信息"));
                }
            }

            UserResponse response = UserResponse.fromUser(user);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("用户不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error getting user by id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户信息失败"));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的信息")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未授权"));
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            User user = userService.findByIdWithCompany(userId);

            UserResponse response = UserResponse.fromUser(user);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户信息失败"));
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户信息", description = "更新指定用户的信息")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserCreateRequest request) {
        try {
            User existingUser = userService.findById(id);

            // 检查用户名是否被其他用户使用
            if (!existingUser.getUsername().equals(request.getUsername()) &&
                userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("用户名已被其他用户使用"));
            }

            // 检查邮箱是否被其他用户使用
            if (request.getEmail() != null &&
                !request.getEmail().equals(existingUser.getEmail()) &&
                userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("邮箱已被其他用户使用"));
            }

            // 更新用户基本信息
            existingUser.setUsername(request.getUsername());
            existingUser.setFullName(request.getFullName());
            existingUser.setEmail(request.getEmail());

            // 如果提供了新密码，则更新密码
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            User updatedUser = userService.updateUser(id, existingUser);
            UserResponse response = UserResponse.fromUser(updatedUser);

            log.info("User updated successfully: {}", updatedUser.getUsername());
            return ResponseEntity.ok(ApiResponse.updated(response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("用户不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error updating user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新用户信息失败"));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除用户", description = "删除指定的用户")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        try {
            // 不允许删除自己
            String token = extractTokenFromRequest(null);
            if (token != null) {
                Long currentUserId = jwtUtil.getUserIdFromToken(token);
                if (currentUserId.equals(id)) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.badRequest("不能删除自己的账号"));
                }
            }

            userService.deleteUser(id);
            log.info("User deleted successfully: {}", id);
            return ResponseEntity.ok(ApiResponse.deleted());

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("用户不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error deleting user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除用户失败"));
        }
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户列表", description = "分页获取所有用户列表")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "企业ID筛选") @RequestParam(required = false) Long companyId) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<User> users;
            if (companyId != null) {
                // 按企业筛选
                users = userService.findByCompanyId(companyId, pageable);
            } else {
                // 获取所有用户
                users = userService.findAll(pageable);
            }

            Page<UserResponse> response = users.map(UserResponse::fromUser);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户列表失败"));
        }
    }

    /**
     * 根据用户名搜索用户
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "搜索用户", description = "根据用户名搜索用户")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @Parameter(description = "用户名关键词") @RequestParam String username,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<User> users = userService.findByUsernameContaining(username, pageable);
            Page<UserResponse> response = users.map(UserResponse::fromUser);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索用户失败"));
        }
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户状态", description = "更新用户的启用/禁用状态")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户状态") @RequestParam UserStatus status) {
        try {
            // 不允许禁用自己
            String token = extractTokenFromRequest(null);
            if (token != null) {
                Long currentUserId = jwtUtil.getUserIdFromToken(token);
                if (currentUserId.equals(id) && status == UserStatus.INACTIVE) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.badRequest("不能禁用自己的账号"));
                }
            }

            userService.updateUserStatus(id, status);
            log.info("User status updated successfully: {} -> {}", id, status);
            return ResponseEntity.ok(ApiResponse.success("用户状态更新成功"));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("用户不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error updating user status {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新用户状态失败"));
        }
    }
}
