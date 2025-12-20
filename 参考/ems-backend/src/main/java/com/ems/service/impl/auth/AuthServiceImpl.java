package com.ems.service.impl.auth;

import com.ems.dto.auth.LoginRequest;
import com.ems.dto.auth.LoginResponse;
import com.ems.dto.auth.RegisterRequest;
import com.ems.entity.enterprise.Enterprise;
import com.ems.entity.User;
import com.ems.repository.enterprise.EnterpriseRepository;
import com.ems.repository.UserRepository;
import com.ems.security.JwtTokenProvider;
import com.ems.security.CustomUserDetailsService;
import com.ems.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 *
 * @author EMS Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;

    @Value("${jwt.expiration:86400000}") // 24小时
    private long jwtExpirationInMs;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("User login attempt: {}", loginRequest.getUsername());

        try {
            // 进行身份认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 获取用户详情
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = (User) userDetails;

            // 获取企业名称
            String enterpriseName = null;
            if (user.getEnterpriseId() != null) {
                enterpriseName = enterpriseRepository.findById(user.getEnterpriseId())
                        .map(Enterprise::getName)
                        .orElse(null);
            }

            // 生成令牌
            String accessToken = tokenProvider.generateAccessToken(userDetails, user.getEnterpriseId());
            String refreshToken = tokenProvider.generateRefreshToken(userDetails, user.getEnterpriseId());

            // 更新最后登录时间
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // 构建响应
            LoginResponse response = LoginResponse.fromUser(
                    user, accessToken, refreshToken, jwtExpirationInMs / 1000, enterpriseName);

            log.info("User logged in successfully: {}", user.getUsername());
            return response;

        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", loginRequest.getUsername());
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    @Override
    @Transactional
    public String register(RegisterRequest registerRequest) {
        log.info("User registration attempt: {}", registerRequest.getUsername());

        // 验证密码匹配
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsernameAndDeletedFalse(registerRequest.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmailAndDeletedFalse(registerRequest.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在");
        }

        // 验证企业用户和管理员必须关联企业
        User.UserRole role = registerRequest.getRole();
        if ((role == User.UserRole.ENTERPRISE_ADMIN || role == User.UserRole.ENTERPRISE_USER)
                && registerRequest.getEnterpriseId() == null) {
            throw new IllegalArgumentException("企业用户和管理员必须关联企业");
        }

        // 验证企业是否存在
        if (registerRequest.getEnterpriseId() != null) {
            if (!enterpriseRepository.existsById(registerRequest.getEnterpriseId())) {
                throw new IllegalArgumentException("企业不存在");
            }
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());
        user.setRole(role);
        user.setEnterpriseId(registerRequest.getEnterpriseId());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());
        return user.getId().toString();
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token");

        try {
            // 验证刷新令牌格式
            if (!tokenProvider.validateTokenFormat(refreshToken)) {
                throw new BadCredentialsException("无效的刷新令牌");
            }

            // 提取用户名
            String username = tokenProvider.getUsernameFromToken(refreshToken);

            // 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            User user = (User) userDetails;

            // 验证刷新令牌
            if (!tokenProvider.validateToken(refreshToken, userDetails)) {
                throw new BadCredentialsException("刷新令牌已过期或无效");
            }

            // 获取企业名称
            String enterpriseName = null;
            if (user.getEnterpriseId() != null) {
                enterpriseName = enterpriseRepository.findById(user.getEnterpriseId())
                        .map(Enterprise::getName)
                        .orElse(null);
            }

            // 生成新的令牌
            String newAccessToken = tokenProvider.generateAccessToken(userDetails, user.getEnterpriseId());
            String newRefreshToken = tokenProvider.generateRefreshToken(userDetails, user.getEnterpriseId());

            // 构建响应
            LoginResponse response = LoginResponse.fromUser(
                    user, newAccessToken, newRefreshToken, jwtExpirationInMs / 1000, enterpriseName);

            log.debug("Token refreshed successfully for user: {}", user.getUsername());
            return response;

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("令牌刷新失败");
        }
    }

    @Override
    public void logout(String token) {
        log.info("User logout");
        // 在实际应用中，可以将令牌加入黑名单或使用其他方式使令牌失效
        // 这里简化处理，前端删除令牌即可
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameAndDeletedFalse(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndDeletedFalse(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getAvailableEnterprises() {
        return enterpriseRepository.findAll().stream()
                .filter(enterprise -> !enterprise.getDeleted())
                .map(enterprise -> {
                    return new Object() {
                        public Long id = enterprise.getId();
                        public String name = enterprise.getName();
                        public String description = "";  // Enterprise实体没有description字段
                    };
                })
                .toList();
    }

    @Override
    @Transactional
    public void submitPasswordResetRequest(String username, String email, String reason) {
        log.info("Password reset request submitted for user: {}, email: {}", username, email);

        // 验证用户是否存在（用户名和邮箱匹配）
        User user = userRepository.findByUsernameOrEmailAndDeletedFalse(username, email)
                .filter(u -> username.equals(u.getUsername()) && email.equals(u.getEmail()))
                .orElse(null);

        if (user == null) {
            // 为了安全，即使用户不存在也记录请求但不抛出异常
            log.warn("Password reset request for non-existent user: {} or email mismatch", username);
            return;
        }

        // 在实际应用中，这里可以记录到数据库或发送通知给管理员
        // 目前简化处理，仅记录日志
        log.info("Password reset request logged for user: {}, reason: {}", username, reason);
    }
}