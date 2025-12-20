package com.ems.controller.auth;

import com.ems.dto.common.ApiResponse;
import com.ems.entity.User;
import com.ems.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 密码重置控制器
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "密码重置", description = "密码重置相关接口")
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 忘记密码请求
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "发送密码重置请求")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        try {
            User user = userRepository.findByUsernameOrEmailAndDeletedFalse(request.getEmail(), request.getEmail())
                    .orElse(null);

            if (user == null) {
                // 为了安全，即使用户不存在也返回成功消息
                return ResponseEntity.ok(ApiResponse.success("如果邮箱存在，密码重置链接已发送", ""));
            }

            // 在实际应用中，这里应该发送邮件或短信
            // 为了演示，我们直接返回成功消息
            log.info("Password reset requested for user: {}", user.getUsername());

            return ResponseEntity.ok(ApiResponse.success("如果邮箱存在，密码重置链接已发送", ""));

        } catch (Exception e) {
            log.error("Forgot password failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("密码重置请求失败: " + e.getMessage()));
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "使用重置令牌重置密码")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset request for username: {}", request.getUsername());

        try {
            User user = userRepository.findByUsernameOrEmailAndDeletedFalse(request.getUsername(), request.getUsername())
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }

            // 在实际应用中，这里应该验证重置令牌
            // 为了演示，我们直接重置密码
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setCredentialsNonExpired(true);
            userRepository.save(user);

            log.info("Password reset successful for user: {}", user.getUsername());

            return ResponseEntity.ok(ApiResponse.success("密码重置成功", ""));

        } catch (Exception e) {
            log.error("Reset password failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("密码重置失败: " + e.getMessage()));
        }
    }

    /**
     * 验证重置令牌
     */
    @GetMapping("/verify-reset-token")
    @Operation(summary = "验证重置令牌", description = "验证密码重置令牌是否有效")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetToken(@RequestParam String token) {
        try {
            // 在实际应用中，这里应该验证令牌的有效性和过期时间
            // 为了演示，我们直接返回true
            return ResponseEntity.ok(ApiResponse.success("令牌验证完成", true));

        } catch (Exception e) {
            log.error("Verify reset token failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("验证令牌失败: " + e.getMessage()));
        }
    }

    // DTO类
    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "邮箱不能为空")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        private String newPassword;

        private String resetToken;
    }
}