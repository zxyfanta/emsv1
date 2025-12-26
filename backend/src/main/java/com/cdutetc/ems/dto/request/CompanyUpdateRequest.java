package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新企业信息请求DTO (普通用户专用)
 * 只包含允许普通用户修改的字段
 */
@Data
public class CompanyUpdateRequest {

    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String companyName;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String contactEmail;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String contactPhone;

    @Size(max = 255, message = "企业地址长度不能超过255个字符")
    private String address;

    @Size(max = 500, message = "企业描述长度不能超过500个字符")
    private String description;
}
