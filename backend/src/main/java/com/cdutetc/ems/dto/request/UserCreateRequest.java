package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户请求DTO
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6个字符")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 100, message = "全名长度不能超过100个字符")
    private String fullName;

    @NotBlank(message = "所属企业不能为空")
    private Long companyId;
}