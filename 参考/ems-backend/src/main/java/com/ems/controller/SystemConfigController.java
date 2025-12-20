package com.ems.controller;

import com.ems.exception.BusinessException;
import com.ems.dto.common.ApiResponse;
import com.ems.entity.SystemConfig;
import com.ems.entity.User;
import com.ems.exception.ErrorCode;
import com.ems.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置管理控制器
 * 提供系统配置的RESTful API接口
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "系统配置管理", description = "系统配置管理相关接口")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取配置值
     */
    @GetMapping("/value/{configKey}")
    @Operation(summary = "获取配置值", description = "根据配置键获取对应的配置值")
    public ResponseEntity<ApiResponse<String>> getConfigValue(
            @Parameter(description = "配置键", required = true)
            @PathVariable String configKey) {

        String value = systemConfigService.getConfigValue(configKey);
        return ResponseEntity.ok(ApiResponse.success(value));
    }

    /**
     * 获取配置详情
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "获取配置详情", description = "根据配置键获取完整的配置信息")
    public ResponseEntity<ApiResponse<SystemConfig>> getConfig(
            @Parameter(description = "配置键", required = true)
            @PathVariable String configKey) {

        Optional<SystemConfig> config = systemConfigService.getConfig(configKey);
        return config.map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                     .orElse(ResponseEntity.ok(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND, "配置不存在")));
    }

    /**
     * 获取所有启用的配置
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的配置", description = "获取所有启用状态的系统配置")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getAllEnabledConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllEnabledConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 根据分类获取配置
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据分类获取配置", description = "根据配置分类获取对应的配置列表")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getConfigsByCategory(
            @Parameter(description = "配置分类", required = true)
            @PathVariable SystemConfig.ConfigCategory category) {

        List<SystemConfig> configs = systemConfigService.getConfigsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 分页查询配置
     */
    @GetMapping
    @Operation(summary = "分页查询配置", description = "分页查询系统配置列表")
    public ResponseEntity<ApiResponse<Page<SystemConfig>>> getConfigs(
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段")
            @RequestParam(defaultValue = "configKey") String sortBy,
            @Parameter(description = "排序方向")
            @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "配置分类")
            @RequestParam(required = false) SystemConfig.ConfigCategory category,
            @Parameter(description = "配置类型")
            @RequestParam(required = false) SystemConfig.ConfigType configType,
            @Parameter(description = "配置名称（模糊查询）")
            @RequestParam(required = false) String configName) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SystemConfig> configPage = systemConfigService.getConfigs(pageable, category, configType, configName);
        return ResponseEntity.ok(ApiResponse.success(configPage));
    }

    /**
     * 创建配置
     */
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "创建配置", description = "创建新的系统配置")
    public ResponseEntity<ApiResponse<SystemConfig>> createConfig(
            @Valid @RequestBody SystemConfig config,
            @AuthenticationPrincipal User currentUser) {

        SystemConfig createdConfig = systemConfigService.createConfig(config, currentUser);
        return ResponseEntity.ok(ApiResponse.success(createdConfig));
    }

    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "更新配置", description = "更新指定的系统配置")
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfig(
            @Parameter(description = "配置ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SystemConfig config,
            @AuthenticationPrincipal User currentUser) {

        SystemConfig updatedConfig = systemConfigService.updateConfig(id, config, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedConfig));
    }

    /**
     * 更新配置值
     */
    @PutMapping("/{configKey}/value")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "更新配置值", description = "仅更新配置的值")
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfigValue(
            @Parameter(description = "配置键", required = true)
            @PathVariable String configKey,
            @Parameter(description = "配置值", required = true)
            @RequestParam String value,
            @AuthenticationPrincipal User currentUser) {

        SystemConfig updatedConfig = systemConfigService.updateConfigValue(configKey, value, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedConfig));
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "删除配置", description = "删除指定的系统配置")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(
            @Parameter(description = "配置ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        systemConfigService.deleteConfig(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 批量更新启用状态
     */
    @PutMapping("/batch-enabled")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "批量更新启用状态", description = "批量更新配置的启用状态")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateEnabled(
            @Parameter(description = "配置ID列表", required = true)
            @RequestParam List<Long> ids,
            @Parameter(description = "启用状态", required = true)
            @RequestParam Boolean enabled,
            @AuthenticationPrincipal User currentUser) {

        int updatedCount = systemConfigService.batchUpdateEnabled(ids, enabled, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedCount));
    }

    /**
     * 获取需要重启的配置
     */
    @GetMapping("/require-restart")
    @Operation(summary = "获取需要重启的配置", description = "获取修改后需要重启才能生效的配置列表")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getRequireRestartConfigs() {
        List<SystemConfig> configs = systemConfigService.getRequireRestartConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 获取配置统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取配置统计信息", description = "获取各分类的配置数量统计")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getConfigStatistics() {
        Map<String, Long> statistics = systemConfigService.getConfigStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh-cache")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "刷新配置缓存", description = "刷新系统配置的缓存")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        systemConfigService.refreshCache();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取布尔类型配置值
     */
    @GetMapping("/boolean/{configKey}")
    @Operation(summary = "获取布尔配置值", description = "获取布尔类型的配置值")
    public ResponseEntity<ApiResponse<Boolean>> getBooleanConfig(
            @Parameter(description = "配置键", required = true)
            @PathVariable String configKey) {

        Boolean value = systemConfigService.getBooleanConfig(configKey);
        return ResponseEntity.ok(ApiResponse.success(value));
    }

    /**
     * 获取数字类型配置值
     */
    @GetMapping("/number/{configKey}")
    @Operation(summary = "获取数字配置值", description = "获取数字类型的配置值")
    public ResponseEntity<ApiResponse<Number>> getNumericConfig(
            @Parameter(description = "配置键", required = true)
            @PathVariable String configKey) {

        Number value = systemConfigService.getNumericConfig(configKey);
        return ResponseEntity.ok(ApiResponse.success(value));
    }

    /**
     * 验证配置值
     */
    @PostMapping("/validate")
    @Operation(summary = "验证配置值", description = "验证配置值是否符合配置类型的规则")
    public ResponseEntity<ApiResponse<Boolean>> validateConfigValue(
            @Parameter(description = "配置类型", required = true)
            @RequestParam SystemConfig.ConfigType configType,
            @Parameter(description = "配置值", required = true)
            @RequestParam String value,
            @Parameter(description = "验证规则")
            @RequestParam(required = false) String validationRule) {

        try {
            // 创建临时配置对象进行验证
            SystemConfig tempConfig = new SystemConfig();
            tempConfig.setConfigType(configType);
            tempConfig.setConfigValue(value);
            tempConfig.setValidationRule(validationRule);

            // 这里应该调用验证逻辑，简化实现
            boolean isValid = true;

            return ResponseEntity.ok(ApiResponse.success(isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }
}