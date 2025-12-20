package com.ems.controller.enterprise;

import com.ems.dto.common.ApiResponse;
import com.ems.dto.common.PageResponse;
import com.ems.dto.enterprise.EnterpriseDTO;
import com.ems.entity.enterprise.Enterprise;
import com.ems.service.enterprise.EnterpriseService;
import com.ems.common.utils.SecurityUtils;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 企业管理控制器
 *
 * @author EMS Team
 */
@Slf4j
@RestController
@RequestMapping("/enterprises")
@RequiredArgsConstructor
@Tag(name = "企业管理", description = "企业信息管理相关接口")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    /**
     * 获取企业列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取企业列表", description = "分页获取企业列表，支持关键词搜索和状态筛选")
    public ResponseEntity<ApiResponse<PageResponse<EnterpriseDTO>>> getEnterprises(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "企业状态") @RequestParam(required = false) String status) {

        log.info("获取企业列表请求: page={}, size={}, keyword={}, status={}", page, size, keyword, status);

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            Page<EnterpriseDTO> enterprises = enterpriseService.getEnterprises(
                    page, size, sortBy, sortDir, keyword, status, userRole, userEnterpriseId);

            PageResponse<EnterpriseDTO> pageResponse = new PageResponse<>(
                    enterprises.getContent(),
                    enterprises.getNumber(),
                    enterprises.getSize(),
                    enterprises.getTotalElements(),
                    enterprises.getTotalPages()
            );

            log.info("获取企业列表成功: total={}", enterprises.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("获取企业列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取企业列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取简化企业列表（用于下拉选择）
     */
    @GetMapping("/simple")
    @Operation(summary = "获取简化企业列表", description = "获取企业ID和名称，用于下拉选择")
    public ResponseEntity<ApiResponse<List<EnterpriseDTO.SimpleEnterpriseDTO>>> getSimpleEnterprises() {
        log.info("获取简化企业列表请求");

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            List<EnterpriseDTO.SimpleEnterpriseDTO> enterprises = enterpriseService.getSimpleEnterprises(userRole, userEnterpriseId);

            log.info("获取简化企业列表成功: count={}", enterprises.size());
            return ResponseEntity.ok(ApiResponse.success(enterprises));
        } catch (Exception e) {
            log.error("获取简化企业列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取简化企业列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取企业详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取企业详情", description = "根据企业ID获取详细信息")
    public ResponseEntity<ApiResponse<EnterpriseDTO>> getEnterpriseById(
            @Parameter(description = "企业ID") @PathVariable Long id) {

        log.info("获取企业详情请求: id={}", id);

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            Optional<EnterpriseDTO> enterprise = enterpriseService.getEnterpriseById(id, userRole, userEnterpriseId);

            if (enterprise.isPresent()) {
                log.info("获取企业详情成功: id={}", id);
                return ResponseEntity.ok(ApiResponse.success(enterprise.get()));
            } else {
                log.warn("企业不存在或无权限访问: id={}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取企业详情失败: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取企业详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建企业
     */
    @PostMapping
    @Operation(summary = "创建企业", description = "创建新的企业")
    public ResponseEntity<ApiResponse<EnterpriseDTO>> createEnterprise(
            @Valid @RequestBody Enterprise enterprise) {

        log.info("创建企业请求: name={}", enterprise.getName());

        try {
            Enterprise createdEnterprise = enterpriseService.createEnterprise(enterprise);

            EnterpriseDTO responseDTO = EnterpriseDTO.builder()
                    .id(createdEnterprise.getId())
                    .name(createdEnterprise.getName())
                    .createdAt(createdEnterprise.getCreatedAt())
                    .build();

            log.info("创建企业成功: id={}, name={}", createdEnterprise.getId(), createdEnterprise.getName());
            return ResponseEntity.ok(ApiResponse.success(responseDTO));
        } catch (IllegalArgumentException e) {
            log.warn("创建企业失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("创建企业失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("创建企业失败: " + e.getMessage()));
        }
    }

    /**
     * 更新企业
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新企业", description = "更新企业信息")
    public ResponseEntity<ApiResponse<EnterpriseDTO>> updateEnterprise(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @Valid @RequestBody Enterprise enterprise) {

        log.info("更新企业请求: id={}, name={}", id, enterprise.getName());

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            Optional<Enterprise> updatedEnterprise = enterpriseService.updateEnterprise(
                    id, enterprise, userRole, userEnterpriseId);

            if (updatedEnterprise.isPresent()) {
                EnterpriseDTO responseDTO = EnterpriseDTO.builder()
                        .id(updatedEnterprise.get().getId())
                        .name(updatedEnterprise.get().getName())
                        .createdAt(updatedEnterprise.get().getCreatedAt())
                        .build();

                log.info("更新企业成功: id={}, name={}", updatedEnterprise.get().getId(), updatedEnterprise.get().getName());
                return ResponseEntity.ok(ApiResponse.success(responseDTO));
            } else {
                log.warn("企业不存在或无权限更新: id={}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            log.warn("更新企业失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("更新企业失败: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("更新企业失败: " + e.getMessage()));
        }
    }

    /**
     * 删除企业（软删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除企业", description = "软删除企业")
    public ResponseEntity<ApiResponse<Void>> deleteEnterprise(
            @Parameter(description = "企业ID") @PathVariable Long id) {

        log.info("删除企业请求: id={}", id);

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            boolean deleted = enterpriseService.deleteEnterprise(id, userRole, userEnterpriseId);

            if (deleted) {
                log.info("删除企业成功: id={}", id);
                return ResponseEntity.ok(ApiResponse.success(null));
            } else {
                log.warn("企业不存在或无权限删除: id={}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("删除企业失败: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除企业失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索企业
     */
    @GetMapping("/search")
    @Operation(summary = "搜索企业", description = "根据关键词搜索企业")
    public ResponseEntity<ApiResponse<List<EnterpriseDTO>>> searchEnterprises(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {

        log.info("搜索企业请求: keyword={}", keyword);

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            List<EnterpriseDTO> enterprises = enterpriseService.searchEnterprises(keyword, userRole, userEnterpriseId);

            log.info("搜索企业成功: keyword={}, count={}", keyword, enterprises.size());
            return ResponseEntity.ok(ApiResponse.success(enterprises));
        } catch (Exception e) {
            log.error("搜索企业失败: keyword={}", keyword, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索企业失败: " + e.getMessage()));
        }
    }

    /**
     * 获取企业统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取企业统计", description = "获取企业数量统计信息")
    public ResponseEntity<ApiResponse<EnterpriseDTO.EnterpriseStatsDTO>> getEnterpriseStats() {
        log.info("获取企业统计数据请求");

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            EnterpriseDTO.EnterpriseStatsDTO stats = enterpriseService.getEnterpriseStats(userRole, userEnterpriseId);

            log.info("获取企业统计成功: totalCount={}", stats.getTotalCount());
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取企业统计失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取企业统计失败: " + e.getMessage()));
        }
    }
}