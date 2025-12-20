package com.ems.service.auth;

import com.ems.dto.auth.LoginRequest;
import com.ems.dto.auth.LoginResponse;
import com.ems.dto.auth.RegisterRequest;

/**
 * 认证服务接口
 *
 * @author EMS Team
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 用户ID
     */
    String register(RegisterRequest registerRequest);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的登录响应
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     *
     * @param token 当前令牌
     */
    void logout(String token);

    /**
     * 检查用户名是否可用
     *
     * @param username 用户名
     * @return 是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     *
     * @param email 邮箱
     * @return 是否可用
     */
    boolean isEmailAvailable(String email);

    /**
     * 获取可用企业列表
     *
     * @return 企业列表
     */
    Object getAvailableEnterprises();

    /**
     * 提交密码重置请求
     *
     * @param username 用户名
     * @param email 邮箱
     * @param reason 重置原因
     */
    void submitPasswordResetRequest(String username, String email, String reason);
}