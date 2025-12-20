package com.ems.controller.auth;

import com.ems.dto.auth.LoginRequest;
import com.ems.dto.auth.LoginResponse;
import com.ems.dto.auth.RegisterRequest;
import com.ems.dto.common.ApiResponse;
import com.ems.service.auth.AuthService;
import com.ems.entity.User;
import com.ems.repository.UserRepository;
import com.ems.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证控制器
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for user: {}", request.getUsername());

        try {
            LoginResponse response = authService.login(request);
            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.success("登录成功", response));
        } catch (Exception e) {
            log.warn("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("用户名或密码错误"));
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账户")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for user: {}", request.getUsername());

        try {
            String userId = authService.register(request);
            log.info("Registration successful for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("注册成功", "用户ID: " + userId));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("注册失败，请稍后重试"));
        }
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");

        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            log.debug("Token refresh successful");
            return ResponseEntity.ok(ApiResponse.success("令牌刷新成功", response));
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<LoginResponse>error("令牌刷新失败"));
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出，使当前令牌失效")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        log.info("Logout request received");

        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                authService.logout(token);
                log.info("Logout successful");
            }
            return ResponseEntity.ok(ApiResponse.<Void>success("登出成功", null));
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>error("登出失败"));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getCurrentUser(HttpServletRequest request) {
        log.info("Current user info request received");

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                log.warn("No authentication token provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("未提供认证令牌"));
            }

            log.info("Token extracted: {}", token.substring(0, Math.min(token.length(), 20)) + "...");

            // 从token解析用户信息
            String username = jwtTokenProvider.getUsernameFromToken(token);
            log.info("Username extracted from token: {}", username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

            log.info("User found: {}, role: {}", user.getUsername(), user.getRole());

            // 转换为UserInfo响应
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setFullName(user.getFullName());
            userInfo.setEmail(user.getEmail());
            userInfo.setRole(user.getRole());
            userInfo.setEnterpriseId(user.getEnterprise() != null ? user.getEnterprise().getId() : null);
            userInfo.setEnterpriseName(user.getEnterprise() != null ? user.getEnterprise().getName() : null);

            // 设置权限信息
            List<String> authorities = new ArrayList<>();
            authorities.add("ROLE_" + user.getRole().name());
            userInfo.setAuthorities(authorities);

            log.info("User info prepared successfully for: {}", username);
            return ResponseEntity.ok(ApiResponse.<LoginResponse.UserInfo>success("获取用户信息成功", userInfo));

        } catch (Exception e) {
            log.error("Get current user info error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<LoginResponse.UserInfo>error("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名可用性", description = "检查用户名是否已被注册")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(@RequestParam String username) {
        try {
            boolean available = authService.isUsernameAvailable(username);
            return ResponseEntity.ok(ApiResponse.success("检查完成", available));
        } catch (Exception e) {
            log.error("Check username availability error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败"));
        }
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱可用性", description = "检查邮箱是否已被注册")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(@RequestParam String email) {
        try {
            boolean available = authService.isEmailAvailable(email);
            return ResponseEntity.ok(ApiResponse.success("检查完成", available));
        } catch (Exception e) {
            log.error("Check email availability error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败"));
        }
    }

    /**
     * 获取企业列表（用于注册时选择）
     */
    @GetMapping("/enterprises")
    @Operation(summary = "获取企业列表", description = "获取所有可用企业列表")
    public ResponseEntity<ApiResponse<Object>> getEnterprises() {
        try {
            var enterprises = authService.getAvailableEnterprises();
            return ResponseEntity.ok(ApiResponse.success("获取企业列表成功", enterprises));
        } catch (Exception e) {
            log.error("Get enterprises failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取企业列表失败: " + e.getMessage()));
        }
    }

    /**
     * 密码重置请求（仅提交请求，不自动重置）
     */
    @PostMapping("/request-password-reset")
    @Operation(summary = "请求密码重置", description = "提交密码重置请求，等待管理员处理")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        log.info("Password reset request received for user: {}", request.getUsername());

        try {
            authService.submitPasswordResetRequest(request.getUsername(), request.getEmail(), request.getRequestReason());
            return ResponseEntity.ok(ApiResponse.success("密码重置请求已提交，请联系管理员处理", ""));
        } catch (Exception e) {
            log.error("Password reset request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("提交密码重置请求失败: " + e.getMessage()));
        }
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 刷新令牌请求DTO
     */
    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    /**
     * 密码重置请求DTO
     */
    public static class PasswordResetRequestDto {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "邮箱不能为空")
        private String email;

        private String requestReason;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRequestReason() {
            return requestReason;
        }

        public void setRequestReason(String requestReason) {
            this.requestReason = requestReason;
        }
    }
}