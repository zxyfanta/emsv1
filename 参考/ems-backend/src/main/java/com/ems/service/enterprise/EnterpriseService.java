package com.ems.service.enterprise;

import com.ems.dto.enterprise.EnterpriseDTO;
import com.ems.entity.enterprise.Enterprise;
import com.ems.repository.enterprise.EnterpriseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 企业服务类
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;

    /**
     * 获取企业列表（分页）
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param sortDir 排序方向
     * @param keyword 搜索关键词
     * @param status 状态筛选
     * @param userRole 用户角色
     * @param enterpriseId 用户所属企业ID
     * @return 企业分页数据
     */
    @Transactional(readOnly = true)
    public Page<EnterpriseDTO> getEnterprises(
            int page, int size, String sortBy, String sortDir,
            String keyword, String status, String userRole, Long enterpriseId) {

        log.debug("获取企业列表: page={}, size={}, keyword={}, status={}, role={}, enterpriseId={}",
                page, size, keyword, status, userRole, enterpriseId);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Enterprise> enterprises;

        if ("PLATFORM_ADMIN".equals(userRole)) {
            // 平台管理员可以看到所有企业
            enterprises = enterpriseRepository.findAllActive(keyword, status, pageable);
        } else if (enterpriseId != null) {
            // 企业用户只能看到自己的企业
            enterprises = enterpriseRepository.findByIdAndDeletedFalse(enterpriseId)
                    .map(enterprise -> new org.springframework.data.domain.PageImpl<>(List.of(enterprise), pageable, 1))
                    .orElse(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
        } else {
            enterprises = new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
        }

        return enterprises.map(this::convertToDTO);
    }

    /**
     * 获取简化企业列表（用于下拉选择）
     *
     * @param userRole 用户角色
     * @param enterpriseId 用户所属企业ID
     * @return 简化企业列表
     */
    @Transactional(readOnly = true)
    public List<EnterpriseDTO.SimpleEnterpriseDTO> getSimpleEnterprises(String userRole, Long enterpriseId) {
        log.debug("获取简化企业列表: role={}, enterpriseId={}", userRole, enterpriseId);

        List<Enterprise> enterprises;

        if ("PLATFORM_ADMIN".equals(userRole)) {
            // 平台管理员可以看到所有企业
            enterprises = enterpriseRepository.findAllActive();
        } else if (enterpriseId != null) {
            // 企业用户只能看到自己的企业
            enterprises = enterpriseRepository.findByIdAndDeletedFalse(enterpriseId)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            enterprises = List.of();
        }

        return enterprises.stream()
                .map(enterprise -> EnterpriseDTO.SimpleEnterpriseDTO.builder()
                        .id(enterprise.getId())
                        .name(enterprise.getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取企业
     *
     * @param id 企业ID
     * @param userRole 用户角色
     * @param userEnterpriseId 用户所属企业ID
     * @return 企业信息
     */
    @Transactional(readOnly = true)
    public Optional<EnterpriseDTO> getEnterpriseById(Long id, String userRole, Long userEnterpriseId) {
        log.debug("获取企业详情: id={}, role={}, userEnterpriseId={}", id, userRole, userEnterpriseId);

        return enterpriseRepository.findByIdAndDeletedFalse(id)
                .filter(enterprise -> {
                    // 权限检查：平台管理员可以看到所有企业，企业用户只能看到自己的企业
                    if ("PLATFORM_ADMIN".equals(userRole)) {
                        return true;
                    }
                    return enterprise.getId().equals(userEnterpriseId);
                })
                .map(this::convertToDTO);
    }

    /**
     * 创建企业
     *
     * @param enterprise 企业信息
     * @return 创建后的企业信息
     */
    @Transactional
    public Enterprise createEnterprise(Enterprise enterprise) {
        log.info("创建企业: name={}", enterprise.getName());

        // 检查企业名称是否已存在
        if (enterpriseRepository.findByNameAndDeletedFalse(enterprise.getName()).isPresent()) {
            throw new IllegalArgumentException("企业名称已存在: " + enterprise.getName());
        }

        enterprise.setDeleted(false);
        Enterprise savedEnterprise = enterpriseRepository.save(enterprise);

        log.info("企业创建成功: id={}, name={}", savedEnterprise.getId(), savedEnterprise.getName());
        return savedEnterprise;
    }

    /**
     * 更新企业
     *
     * @param id 企业ID
     * @param enterprise 更新后的企业信息
     * @param userRole 用户角色
     * @param userEnterpriseId 用户所属企业ID
     * @return 更新后的企业信息
     */
    @Transactional
    public Optional<Enterprise> updateEnterprise(Long id, Enterprise enterprise, String userRole, Long userEnterpriseId) {
        log.debug("更新企业: id={}, role={}, userEnterpriseId={}", id, userRole, userEnterpriseId);

        return enterpriseRepository.findByIdAndDeletedFalse(id)
                .filter(existingEnterprise -> {
                    // 权限检查：平台管理员可以更新所有企业，企业用户只能更新自己的企业
                    if ("PLATFORM_ADMIN".equals(userRole)) {
                        return true;
                    }
                    return existingEnterprise.getId().equals(userEnterpriseId);
                })
                .map(existingEnterprise -> {
                    // 检查企业名称是否已被其他企业使用
                    if (!existingEnterprise.getName().equals(enterprise.getName()) &&
                        enterpriseRepository.findByNameAndDeletedFalse(enterprise.getName()).isPresent()) {
                        throw new IllegalArgumentException("企业名称已存在: " + enterprise.getName());
                    }

                    existingEnterprise.setName(enterprise.getName());
                    Enterprise updatedEnterprise = enterpriseRepository.save(existingEnterprise);
                    log.info("企业更新成功: id={}, name={}", updatedEnterprise.getId(), updatedEnterprise.getName());
                    return updatedEnterprise;
                });
    }

    /**
     * 删除企业（软删除）
     *
     * @param id 企业ID
     * @param userRole 用户角色
     * @param userEnterpriseId 用户所属企业ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteEnterprise(Long id, String userRole, Long userEnterpriseId) {
        log.debug("删除企业: id={}, role={}, userEnterpriseId={}", id, userRole, userEnterpriseId);

        return enterpriseRepository.findByIdAndDeletedFalse(id)
                .filter(enterprise -> {
                    // 权限检查：平台管理员可以删除所有企业，企业用户只能删除自己的企业
                    if ("PLATFORM_ADMIN".equals(userRole)) {
                        return true;
                    }
                    return enterprise.getId().equals(userEnterpriseId);
                })
                .map(enterprise -> {
                    enterprise.setDeleted(true);
                    enterpriseRepository.save(enterprise);
                    log.info("企业删除成功: id={}, name={}", enterprise.getId(), enterprise.getName());
                    return true;
                })
                .orElse(false);
    }

    /**
     * 搜索企业
     *
     * @param keyword 搜索关键词
     * @param userRole 用户角色
     * @param enterpriseId 用户所属企业ID
     * @return 搜索结果
     */
    @Transactional(readOnly = true)
    public List<EnterpriseDTO> searchEnterprises(String keyword, String userRole, Long enterpriseId) {
        log.debug("搜索企业: keyword={}, role={}, enterpriseId={}", keyword, userRole, enterpriseId);

        List<Enterprise> enterprises;

        if ("PLATFORM_ADMIN".equals(userRole)) {
            // 平台管理员可以搜索所有企业
            enterprises = enterpriseRepository.findByNameContainingAndDeletedFalse(keyword);
        } else if (enterpriseId != null) {
            // 企业用户只能搜索自己的企业
            enterprises = enterpriseRepository.findByIdAndDeletedFalse(enterpriseId)
                    .filter(enterprise -> enterprise.getName().contains(keyword))
                    .map(List::of)
                    .orElse(List.of());
        } else {
            enterprises = List.of();
        }

        return enterprises.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取企业统计数据
     *
     * @param userRole 用户角色
     * @param enterpriseId 用户所属企业ID
     * @return 统计数据
     */
    @Transactional(readOnly = true)
    public EnterpriseDTO.EnterpriseStatsDTO getEnterpriseStats(String userRole, Long enterpriseId) {
        log.debug("获取企业统计: role={}, enterpriseId={}", userRole, enterpriseId);

        long totalCount;
        long pendingCount = 0;
        long approvedCount = 0;
        long rejectedCount = 0;

        if ("PLATFORM_ADMIN".equals(userRole)) {
            // 平台管理员可以看到所有企业统计
            totalCount = enterpriseRepository.countByDeletedFalse();
            // 注意：这里暂时假设企业实体有status字段，如果没有需要调整
            // pendingCount = enterpriseRepository.countByStatusAndDeletedFalse("PENDING");
            // approvedCount = enterpriseRepository.countByStatusAndDeletedFalse("APPROVED");
            // rejectedCount = enterpriseRepository.countByStatusAndDeletedFalse("REJECTED");

            // 暂时使用简化逻辑
            List<Enterprise> allEnterprises = enterpriseRepository.findAllActive();
            totalCount = allEnterprises.size();

        } else if (enterpriseId != null) {
            // 企业用户只能看到自己的企业统计
            totalCount = enterpriseRepository.findByIdAndDeletedFalse(enterpriseId).isPresent() ? 1 : 0;
            // approvedCount = totalCount; // 用户的自己企业通常是已审核通过的
        } else {
            totalCount = 0;
        }

        return EnterpriseDTO.EnterpriseStatsDTO.builder()
                .totalCount(totalCount)
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .build();
    }

    /**
     * 转换为DTO
     *
     * @param enterprise 企业实体
     * @return 企业DTO
     */
    private EnterpriseDTO convertToDTO(Enterprise enterprise) {
        return EnterpriseDTO.builder()
                .id(enterprise.getId())
                .name(enterprise.getName())
                .createdAt(enterprise.getCreatedAt())
                .build();
    }
}