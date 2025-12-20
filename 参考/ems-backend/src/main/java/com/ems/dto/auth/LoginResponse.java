package com.ems.dto.auth;

import com.ems.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录响应DTO
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "令牌有效期（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfo user;

    /**
     * 用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {

        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "姓名")
        private String fullName;

        @Schema(description = "用户角色")
        private User.UserRole role;

        @Schema(description = "所属企业ID")
        private Long enterpriseId;

        @Schema(description = "所属企业名称")
        private String enterpriseName;

        @Schema(description = "用户权限列表")
        private List<String> authorities;

        @Schema(description = "最后登录时间")
        private LocalDateTime lastLoginAt;
    }

    /**
     * 从User实体创建LoginResponse
     */
    public static LoginResponse fromUser(User user, String accessToken, String refreshToken,
                                        Long expiresIn, String enterpriseName) {
        List<String> authorities = user.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enterpriseId(user.getEnterpriseId())
                .enterpriseName(enterpriseName)
                .authorities(authorities)
                .lastLoginAt(user.getLastLoginAt())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userInfo)
                .build();
    }
}