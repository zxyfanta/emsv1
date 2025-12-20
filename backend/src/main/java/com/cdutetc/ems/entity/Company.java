package com.cdutetc.ems.entity;

import com.cdutetc.ems.entity.enums.CompanyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 企业实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ems_company")
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "企业编码不能为空")
    @Size(max = 50, message = "企业编码长度不能超过50个字符")
    @Column(name = "company_code", nullable = false, unique = true, length = 50)
    private String companyCode;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Size(max = 255, message = "企业地址长度不能超过255个字符")
    @Column(name = "address", length = 255)
    private String address;

    @Size(max = 500, message = "企业描述长度不能超过500个字符")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.ACTIVE;
}