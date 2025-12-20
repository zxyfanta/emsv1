package com.ems.controller.device;

import com.ems.entity.device.DeviceGroup;
import com.ems.entity.enterprise.Enterprise;
import com.ems.repository.enterprise.EnterpriseRepository;
import com.ems.service.DeviceGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 设备分组管理控制器
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/api/device-groups")
@RequiredArgsConstructor
@Slf4j
public class DeviceGroupController {

    private final DeviceGroupService deviceGroupService;
    private final EnterpriseRepository enterpriseRepository;

    /**
     * 获取分组列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    public ResponseEntity<String> getGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long enterpriseId) {
        try {
            // 获取企业信息
            Enterprise enterprise = getCurrentEnterprise(enterpriseId);

            // 创建分页对象
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder", "name"));

            // 获取分页数据
            Page<DeviceGroup> groupPage = deviceGroupService.getGroupsByEnterprise(enterprise, pageable);

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "content", groupPage.getContent(),
                "totalElements", groupPage.getTotalElements(),
                "totalPages", groupPage.getTotalPages(),
                "size", groupPage.getSize(),
                "number", groupPage.getNumber(),
                "first", groupPage.isFirst(),
                "last", groupPage.isLast()
            ));

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (Exception e) {
            log.error("获取设备分组列表失败", e);
            return ResponseEntity.badRequest().body(buildErrorResponse("获取设备分组列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有分组（不分页，用于下拉选择等场景）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    public ResponseEntity<String> getAllGroups(@RequestParam(required = false) Long enterpriseId) {
        try {
            Enterprise enterprise = getCurrentEnterprise(enterpriseId);
            List<DeviceGroup> groups = deviceGroupService.getGroupsByEnterprise(enterprise);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", groups);

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (Exception e) {
            log.error("获取所有设备分组失败", e);
            return ResponseEntity.badRequest().body(buildErrorResponse("获取所有设备分组失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取分组详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    public ResponseEntity<String> getGroupById(@PathVariable Long id) {
        try {
            DeviceGroup group = deviceGroupService.getGroupById(id);

            // 检查权限
            checkGroupPermission(group);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", group);

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("获取设备分组详情失败, ID: {}", id, e);
            return ResponseEntity.badRequest().body(buildErrorResponse("获取设备分组详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建分组
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    public ResponseEntity<String> createGroup(@RequestBody Map<String, Object> groupData) {
        try {
            // 解析请求数据
            String name = (String) groupData.get("name");
            String description = (String) groupData.get("description");
            DeviceGroup.GroupType groupType = DeviceGroup.GroupType.valueOf((String) groupData.get("groupType"));
            Long parentId = groupData.get("parentId") != null ? Long.valueOf(groupData.get("parentId").toString()) : null;
            Long enterpriseId = Long.valueOf(groupData.get("enterpriseId").toString());
            Integer sortOrder = groupData.get("sortOrder") != null ? Integer.valueOf(groupData.get("sortOrder").toString()) : 0;
            Boolean enabled = groupData.get("enabled") != null ? Boolean.valueOf(groupData.get("enabled").toString()) : true;

            // 获取企业信息
            Enterprise enterprise = enterpriseRepository.findByIdAndDeletedFalse(enterpriseId).orElseThrow(
                () -> new IllegalArgumentException("企业不存在")
            );

            // 创建分组对象
            DeviceGroup group = DeviceGroup.builder()
                    .name(name)
                    .description(description)
                    .groupType(groupType)
                    .parentGroup(parentId != null ? deviceGroupService.getGroupById(parentId) : null)
                    .enterprise(enterprise)
                    .sortOrder(sortOrder)
                    .enabled(enabled)
                    .deleted(false)
                    .build();

            // 保存分组
            DeviceGroup createdGroup = deviceGroupService.createGroup(group);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "设备分组创建成功");
            response.put("data", createdGroup);

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (Exception e) {
            log.error("创建设备分组失败", e);
            return ResponseEntity.badRequest().body(buildErrorResponse("创建设备分组失败: " + e.getMessage()));
        }
    }

    /**
     * 更新分组
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    public ResponseEntity<String> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> groupData) {
        try {
            // 获取现有分组
            DeviceGroup existingGroup = deviceGroupService.getGroupById(id);

            // 检查权限
            checkGroupPermission(existingGroup);

            // 解析更新数据
            String name = (String) groupData.get("name");
            String description = (String) groupData.get("description");
            DeviceGroup.GroupType groupType = DeviceGroup.GroupType.valueOf((String) groupData.get("groupType"));
            Long parentId = groupData.get("parentId") != null ? Long.valueOf(groupData.get("parentId").toString()) : null;
            Integer sortOrder = groupData.get("sortOrder") != null ? Integer.valueOf(groupData.get("sortOrder").toString()) : existingGroup.getSortOrder();
            Boolean enabled = groupData.get("enabled") != null ? Boolean.valueOf(groupData.get("enabled").toString()) : existingGroup.getEnabled();

            // 更新分组信息
            existingGroup.setName(name);
            existingGroup.setDescription(description);
            existingGroup.setGroupType(groupType);
            existingGroup.setParentGroup(parentId != null ? deviceGroupService.getGroupById(parentId) : null);
            existingGroup.setSortOrder(sortOrder);
            existingGroup.setEnabled(enabled);

            // 保存更新
            DeviceGroup updatedGroup = deviceGroupService.updateGroup(existingGroup);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "设备分组更新成功");
            response.put("data", updatedGroup);

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("更新设备分组失败, ID: {}", id, e);
            return ResponseEntity.badRequest().body(buildErrorResponse("更新设备分组失败: " + e.getMessage()));
        }
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    public ResponseEntity<String> deleteGroup(@PathVariable Long id) {
        try {
            // 获取分组
            DeviceGroup group = deviceGroupService.getGroupById(id);

            // 检查权限
            checkGroupPermission(group);

            // 检查是否可以删除
            if (!deviceGroupService.canDeleteGroup(id)) {
                return ResponseEntity.badRequest().body(buildErrorResponse("该分组下存在子分组或设备，无法删除"));
            }

            // 删除分组
            deviceGroupService.deleteGroup(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "设备分组删除成功");

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("删除设备分组失败, ID: {}", id, e);
            return ResponseEntity.badRequest().body(buildErrorResponse("删除设备分组失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除分组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    public ResponseEntity<String> batchDeleteGroups(@RequestBody List<Long> groupIds) {
        try {
            int successCount = 0;
            int failCount = 0;

            for (Long groupId : groupIds) {
                try {
                    DeviceGroup group = deviceGroupService.getGroupById(groupId);
                    checkGroupPermission(group);

                    if (deviceGroupService.canDeleteGroup(groupId)) {
                        deviceGroupService.deleteGroup(groupId);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.warn("删除分组失败, ID: {}", groupId, e);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("批量删除完成，成功: %d, 失败: %d", successCount, failCount));
            response.put("data", Map.of(
                "successCount", successCount,
                "failCount", failCount
            ));

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (Exception e) {
            log.error("批量删除设备分组失败", e);
            return ResponseEntity.badRequest().body(buildErrorResponse("批量删除设备分组失败: " + e.getMessage()));
        }
    }

    /**
     * 获取分组统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    public ResponseEntity<String> getGroupStatistics(@RequestParam(required = false) Long enterpriseId) {
        try {
            Enterprise enterprise = getCurrentEnterprise(enterpriseId);
            List<DeviceGroup> groups = deviceGroupService.getGroupsByEnterprise(enterprise);

            long totalGroups = groups.size();
            long enabledGroups = groups.stream().filter(DeviceGroup::getEnabled).count();
            long systemGroups = groups.stream().filter(g -> g.getGroupType() == DeviceGroup.GroupType.SYSTEM).count();
            long customGroups = groups.stream().filter(g -> g.getGroupType() == DeviceGroup.GroupType.CUSTOM).count();

            Map<String, Object> statistics = Map.of(
                "totalGroups", totalGroups,
                "enabledGroups", enabledGroups,
                "disabledGroups", totalGroups - enabledGroups,
                "systemGroups", systemGroups,
                "customGroups", customGroups,
                "locationGroups", groups.stream().filter(g -> g.getGroupType() == DeviceGroup.GroupType.LOCATION).count(),
                "functionGroups", groups.stream().filter(g -> g.getGroupType() == DeviceGroup.GroupType.FUNCTION).count()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (Exception e) {
            log.error("获取设备分组统计信息失败", e);
            return ResponseEntity.badRequest().body(buildErrorResponse("获取设备分组统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索分组
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    public ResponseEntity<String> searchGroups(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long enterpriseId) {
        try {
            Enterprise enterprise = getCurrentEnterprise(enterpriseId);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder", "name"));

            Page<DeviceGroup> searchResult = deviceGroupService.searchGroups(enterprise, keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "content", searchResult.getContent(),
                "totalElements", searchResult.getTotalElements(),
                "totalPages", searchResult.getTotalPages(),
                "size", searchResult.getSize(),
                "number", searchResult.getNumber(),
                "first", searchResult.isFirst(),
                "last", searchResult.isLast()
            ));

            return ResponseEntity.ok(buildJsonResponse(response));

        } catch (Exception e) {
            log.error("搜索设备分组失败", e);
            return ResponseEntity.badRequest().body(buildErrorResponse("搜索设备分组失败: " + e.getMessage()));
        }
    }

    // ============ 私有辅助方法 ============

    /**
     * 获取当前企业信息
     */
    private Enterprise getCurrentEnterprise(Long enterpriseId) {
        if (enterpriseId != null) {
            return enterpriseRepository.findByIdAndDeletedFalse(enterpriseId).orElseThrow(
                () -> new IllegalArgumentException("企业不存在")
            );
        }

        // 如果没有指定企业ID，可以从当前用户上下文获取（这里简化处理）
        // 实际应用中应该从SecurityContext中获取当前用户的企业信息
        return enterpriseRepository.findByIdAndDeletedFalse(1L).orElseThrow(
            () -> new IllegalArgumentException("默认企业不存在")
        ); // 默认企业ID
    }

    /**
     * 检查分组权限
     */
    private void checkGroupPermission(DeviceGroup group) {
        Enterprise currentEnterprise = getCurrentEnterprise(null);
        if (!group.getEnterprise().getId().equals(currentEnterprise.getId())) {
            throw new IllegalArgumentException("无权访问该设备分组");
        }
    }

    /**
     * 构建成功响应JSON
     */
    private String buildJsonResponse(Map<String, Object> data) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"响应数据格式化失败\"}";
        }
    }

    /**
     * 构建错误响应JSON
     */
    private String buildErrorResponse(String message) {
        return String.format("{\"success\":false,\"message\":\"%s\"}", message.replace("\"", "\\\""));
    }
}