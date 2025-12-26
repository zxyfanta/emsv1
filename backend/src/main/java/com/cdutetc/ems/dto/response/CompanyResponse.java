package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 企业响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private Long id;
    private String companyName;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userCount;

    public static CompanyResponse fromCompany(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .address(company.getAddress())
                .description(company.getDescription())
                .status(company.getStatus().name())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    public static CompanyResponse fromCompanyWithUserCount(Company company, Long userCount) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .address(company.getAddress())
                .description(company.getDescription())
                .status(company.getStatus().name())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .userCount(userCount)
                .build();
    }
}