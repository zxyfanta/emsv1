package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.request.CompanyCreateRequest;
import com.cdutetc.ems.dto.response.CompanyResponse;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.repository.UserRepository;
import com.cdutetc.ems.security.JwtUtil;
import com.cdutetc.ems.service.CompanyService;
import com.cdutetc.ems.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@Tag(name = "企业管理", description = "企业管理相关接口")
public class CompanyController {

    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * 获取当前登录用户的企业ID
     */
    private Long getCurrentCompanyId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token != null) {
            return jwtUtil.getCompanyIdFromToken(token);
        }
        throw new RuntimeException("无法获取企业信息");
    }

    /**
     * 验证用户是否有权限访问企业数据
     */
    private boolean validateCompanyAccess(HttpServletRequest request, Long companyId) {
        String token = extractTokenFromRequest(request);
        if (token != null) {
            Long currentCompanyId = jwtUtil.getCompanyIdFromToken(token);
            return currentCompanyId.equals(companyId);
        }
        return false;
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

    /**
     * 创建企业
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建企业", description = "创建新的企业")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CompanyCreateRequest request) {
        try {
            // 检查企业编码是否已存在
            if (companyService.existsByCompanyCode(request.getCompanyCode())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("企业编码已存在"));
            }

            Company company = new Company();
            company.setCompanyCode(request.getCompanyCode());
            company.setCompanyName(request.getCompanyName());
            company.setContactEmail(request.getContactEmail());
            company.setContactPhone(request.getContactPhone());
            company.setAddress(request.getAddress());
            company.setStatus(CompanyStatus.ACTIVE);

            Company createdCompany = companyService.createCompany(company);
            CompanyResponse response = CompanyResponse.fromCompany(createdCompany);

            log.info("Company created successfully: {}", createdCompany.getCompanyCode());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(response));

        } catch (Exception e) {
            log.error("Error creating company: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建企业失败"));
        }
    }

    /**
     * 根据ID获取企业详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取企业详情", description = "根据企业ID获取企业详细信息")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(
            @Parameter(description = "企业ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Company company = companyService.findById(id);

            // 验证权限（管理员可以查看所有企业，普通用户只能查看自己的企业）
            String token = extractTokenFromRequest(request);
            if (token != null) {
                String role = jwtUtil.getRoleFromToken(token);
                if (!"ADMIN".equals(role) && !validateCompanyAccess(request, id)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.forbidden("没有权限访问此企业信息"));
                }
            }

            // 获取企业用户数量
            long userCount = userRepository.countByCompanyId(id);
            CompanyResponse response = CompanyResponse.fromCompanyWithUserCount(company, userCount);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("企业不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error getting company by id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取企业信息失败"));
        }
    }

    /**
     * 获取当前用户的企业信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前企业信息", description = "获取当前登录用户所属企业的信息")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCurrentCompany(HttpServletRequest request) {
        try {
            Long companyId = getCurrentCompanyId(request);
            Company company = companyService.findById(companyId);

            // 获取企业用户数量
            long userCount = userRepository.countByCompanyId(companyId);
            CompanyResponse response = CompanyResponse.fromCompanyWithUserCount(company, userCount);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting current company: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取企业信息失败"));
        }
    }

    /**
     * 更新企业信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新企业信息", description = "更新指定企业的信息")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @Valid @RequestBody CompanyCreateRequest request) {
        try {
            Company existingCompany = companyService.findById(id);

            // 检查企业编码是否被其他企业使用
            if (!existingCompany.getCompanyCode().equals(request.getCompanyCode()) &&
                companyService.existsByCompanyCode(request.getCompanyCode())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("企业编码已被其他企业使用"));
            }

            // 更新企业信息
            existingCompany.setCompanyCode(request.getCompanyCode());
            existingCompany.setCompanyName(request.getCompanyName());
            existingCompany.setContactEmail(request.getContactEmail());
            existingCompany.setContactPhone(request.getContactPhone());
            existingCompany.setAddress(request.getAddress());

            Company updatedCompany = companyService.updateCompany(id, existingCompany);
            CompanyResponse response = CompanyResponse.fromCompany(updatedCompany);

            log.info("Company updated successfully: {}", updatedCompany.getCompanyCode());
            return ResponseEntity.ok(ApiResponse.updated(response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("企业不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error updating company {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新企业信息失败"));
        }
    }

    /**
     * 删除企业
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除企业", description = "删除指定的企业")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(
            @Parameter(description = "企业ID") @PathVariable Long id) {
        try {
            // 检查企业是否还有用户
            long userCount = userRepository.countByCompanyId(id);
            if (userCount > 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("企业下还有用户，无法删除"));
            }

            companyService.deleteCompany(id);
            log.info("Company deleted successfully: {}", id);
            return ResponseEntity.ok(ApiResponse.deleted());

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("企业不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error deleting company {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除企业失败"));
        }
    }

    /**
     * 分页查询企业列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取企业列表", description = "分页获取所有企业列表")
    public ResponseEntity<ApiResponse<Page<CompanyResponse>>> getCompanies(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Company> companies = companyService.findAll(pageable);
            Page<CompanyResponse> response = companies.map(CompanyResponse::fromCompany);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting companies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取企业列表失败"));
        }
    }

    /**
     * 根据企业名称搜索企业
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "搜索企业", description = "根据企业名称搜索企业")
    public ResponseEntity<ApiResponse<Page<CompanyResponse>>> searchCompanies(
            @Parameter(description = "企业名称关键词") @RequestParam String name,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Company> companies = companyService.findByCompanyNameContaining(name, pageable);
            Page<CompanyResponse> response = companies.map(CompanyResponse::fromCompany);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error searching companies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索企业失败"));
        }
    }

    /**
     * 更新企业状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新企业状态", description = "更新企业的启用/禁用状态")
    public ResponseEntity<ApiResponse<Void>> updateCompanyStatus(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @Parameter(description = "企业状态") @RequestParam CompanyStatus status) {
        try {
            companyService.updateCompanyStatus(id, status);
            log.info("Company status updated successfully: {} -> {}", id, status);
            return ResponseEntity.ok(ApiResponse.success("企业状态更新成功"));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("企业不存在"));
            }
            throw e;
        } catch (Exception e) {
            log.error("Error updating company status {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新企业状态失败"));
        }
    }
}