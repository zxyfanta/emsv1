package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.request.LoginRequest;
import com.cdutetc.ems.dto.response.LoginResponse;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.security.JwtUtil;
import com.cdutetc.ems.service.UserService;
import com.cdutetc.ems.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final CompanyRepository companyRepository;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户信息
            User user = (User) authentication.getPrincipal();

            // 生成JWT Token
            String token = jwtUtil.generateToken(user, user.getCompany().getId(), user.getRole().name());

            // 构建响应
            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.fromUser(user);
            LoginResponse loginResponse = LoginResponse.builder()
                    .token(token)
                    .expiresIn(jwtExpiration)
                    .userInfo(userInfo)
                    .build();

            log.info("User {} logged in successfully", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("用户名或密码错误"));
        } catch (AuthenticationException e) {
            log.error("Authentication error for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("认证失败"));
        } catch (Exception e) {
            log.error("Login error for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("登录失败，请稍后重试"));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getCurrentUser(HttpServletRequest request) {
        try {
            // 从请求中获取用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();

            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.fromUser(user);
            return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", userInfo));

        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("获取用户信息失败"));
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("缺少Token"));
            }

            // 验证当前Token
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Token无效或已过期"));
            }

            // 刷新Token
            String refreshedToken = jwtUtil.refreshToken(token);
            if (refreshedToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Token刷新失败"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("token", refreshedToken);
            response.put("expiresIn", jwtExpiration);

            return ResponseEntity.ok(ApiResponse.success("Token刷新成功", response));

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("Token刷新失败"));
        }
    }

    /**
     * 验证Token有效性
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("valid", false);
                errorResponse.put("expiresIn", 0);
                return ResponseEntity.ok(ApiResponse.success("缺少Token", errorResponse));
            }

            boolean isValid = jwtUtil.validateToken(token);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("expiresIn", isValid ? jwtUtil.getExpirationTime(token) : 0);

            return ResponseEntity.ok(ApiResponse.success("Token验证完成", response));

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.ok(ApiResponse.success("Token验证完成", response));
        }
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(ApiResponse.success("登出成功"));
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("登出成功"));
        }
    }

    /**
     * 从请求中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}