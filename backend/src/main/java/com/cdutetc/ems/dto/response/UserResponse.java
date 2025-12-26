package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private CompanyInfo company;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {
        private Long id;
        private String companyName;
        private String contactEmail;
        private String contactPhone;
        private String address;
    }

    public static UserResponse fromUser(User user) {
        CompanyInfo companyInfo = null;
        if (user.getCompany() != null) {
            companyInfo = CompanyInfo.builder()
                    .id(user.getCompany().getId())
                    .companyName(user.getCompany().getCompanyName())
                    .contactEmail(user.getCompany().getContactEmail())
                    .contactPhone(user.getCompany().getContactPhone())
                    .address(user.getCompany().getAddress())
                    .build();
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .company(companyInfo)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}