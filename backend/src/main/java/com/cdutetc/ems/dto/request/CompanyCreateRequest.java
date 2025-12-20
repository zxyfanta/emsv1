package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建企业请求DTO
 */
@Data
public class CompanyCreateRequest {

    @NotBlank(message = "企业编码不能为空")
    @Size(max = 50, message = "企业编码长度不能超过50个字符")
    private String companyCode;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String companyName;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String contactEmail;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String contactPhone;

    @Size(max = 255, message = "企业地址长度不能超过255个字符")
    private String address;
}