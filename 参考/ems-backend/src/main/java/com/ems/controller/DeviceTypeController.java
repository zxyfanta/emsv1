package com.ems.controller.device;

import com.ems.dto.common.ApiResponse;
import com.ems.entity.DeviceType;
import com.ems.service.DeviceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 设备类型管理控制器
 * 提供设备类型的CRUD操作和启用/禁用功能
 *
 * @author EMS Team
 */
@Slf4j
@RestController
@RequestMapping("/device-types")
@RequiredArgsConstructor
@Tag(name = "设备类型管理", description = "设备类型管理相关接口")
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    /**
     * 获取所有设备类型
     */
    @GetMapping
    @Operation(summary = "获取所有设备类型", description = "获取系统中所有设备类型的列表")
    public ResponseEntity<ApiResponse<List<DeviceType>>> getAllDeviceTypes() {
        try {
            List<DeviceType> deviceTypes = deviceTypeService.getAllDeviceTypes();
            return ResponseEntity.ok(ApiResponse.success(deviceTypes));
        } catch (Exception e) {
            log.error("获取设备类型列表失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取设备类型列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取启用的设备类型
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取启用的设备类型", description = "获取所有启用状态的设备类型")
    public ResponseEntity<ApiResponse<List<DeviceType>>> getEnabledDeviceTypes() {
        try {
            List<DeviceType> enabledTypes = deviceTypeService.getEnabledDeviceTypes();
            return ResponseEntity.ok(ApiResponse.success(enabledTypes));
        } catch (Exception e) {
            log.error("获取启用设备类型列表失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取启用设备类型列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据类型代码获取设备类型
     */
    @GetMapping("/{typeCode}")
    @Operation(summary = "获取设备类型详情", description = "根据类型代码获取设备类型的详细信息")
    public ResponseEntity<ApiResponse<DeviceType>> getDeviceType(
            @Parameter(description = "设备类型代码", required = true) @PathVariable String typeCode) {
        try {
            Optional<DeviceType> deviceType = deviceTypeService.getDeviceTypeByCode(typeCode);
            if (deviceType.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(deviceType.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "设备类型不存在: " + typeCode));
            }
        } catch (Exception e) {
            log.error("获取设备类型详情失败: typeCode={}", typeCode, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取设备类型详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建设备类型
     */
    @PostMapping
    @Operation(summary = "创建设备类型", description = "创建新的设备类型")
    public ResponseEntity<ApiResponse<DeviceType>> createDeviceType(
            @Parameter(description = "设备类型信息", required = true) @RequestBody DeviceType deviceType) {
        try {
            DeviceType createdDeviceType = deviceTypeService.createDeviceType(deviceType);
            return ResponseEntity.ok(ApiResponse.success(createdDeviceType));
        } catch (IllegalArgumentException e) {
            log.warn("创建设备类型失败 - 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("创建设备类型失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "创建设备类型失败: " + e.getMessage()));
        }
    }

    /**
     * 更新设备类型
     */
    @PutMapping("/{typeCode}")
    @Operation(summary = "更新设备类型", description = "更新指定设备类型的信息")
    public ResponseEntity<ApiResponse<DeviceType>> updateDeviceType(
            @Parameter(description = "设备类型代码", required = true) @PathVariable String typeCode,
            @Parameter(description = "设备类型更新信息", required = true) @RequestBody DeviceType deviceTypeUpdates) {
        try {
            DeviceType updatedDeviceType = deviceTypeService.updateDeviceType(typeCode, deviceTypeUpdates);
            return ResponseEntity.ok(ApiResponse.success(updatedDeviceType));
        } catch (IllegalArgumentException e) {
            log.warn("更新设备类型失败 - 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("更新设备类型失败: typeCode={}", typeCode, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "更新设备类型失败: " + e.getMessage()));
        }
    }

    /**
     * 启用/禁用设备类型
     */
    @PatchMapping("/{typeCode}/toggle")
    @Operation(summary = "启用/禁用设备类型", description = "切换设备类型的启用状态")
    public ResponseEntity<ApiResponse<DeviceType>> toggleDeviceType(
            @Parameter(description = "设备类型代码", required = true) @PathVariable String typeCode,
            @Parameter(description = "是否启用", required = true) @RequestParam boolean enabled) {
        try {
            DeviceType deviceType = deviceTypeService.toggleDeviceType(typeCode, enabled);
            String action = enabled ? "启用" : "禁用";
            return ResponseEntity.ok(ApiResponse.success("设备类型已成功" + action, deviceType));
        } catch (IllegalArgumentException e) {
            log.warn("切换设备类型状态失败 - 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("切换设备类型状态失败: typeCode={}, enabled={}", typeCode, enabled, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "切换设备类型状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除设备类型
     */
    @DeleteMapping("/{typeCode}")
    @Operation(summary = "删除设备类型", description = "删除指定的设备类型")
    public ResponseEntity<ApiResponse<String>> deleteDeviceType(
            @Parameter(description = "设备类型代码", required = true) @PathVariable String typeCode) {
        try {
            deviceTypeService.deleteDeviceType(typeCode);
            return ResponseEntity.ok(ApiResponse.success("设备类型删除成功"));
        } catch (IllegalArgumentException e) {
            log.warn("删除设备类型失败 - 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("删除设备类型失败: typeCode={}", typeCode, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "删除设备类型失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索设备类型
     */
    @GetMapping("/search")
    @Operation(summary = "搜索设备类型", description = "根据关键词搜索设备类型")
    public ResponseEntity<ApiResponse<List<DeviceType>>> searchDeviceTypes(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword) {
        try {
            List<DeviceType> deviceTypes = deviceTypeService.searchDeviceTypes(keyword);
            return ResponseEntity.ok(ApiResponse.success(deviceTypes));
        } catch (Exception e) {
            log.error("搜索设备类型失败: keyword={}", keyword, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "搜索设备类型失败: " + e.getMessage()));
        }
    }

    /**
     * 根据MQTT主题查找设备类型
     */
    @GetMapping("/match-topic")
    @Operation(summary = "匹配MQTT主题", description = "根据MQTT主题查找匹配的设备类型")
    public ResponseEntity<ApiResponse<DeviceType>> findDeviceTypeByTopic(
            @Parameter(description = "MQTT主题", required = true) @RequestParam String topic) {
        try {
            Optional<DeviceType> deviceType = deviceTypeService.findDeviceTypeByTopic(topic);
            if (deviceType.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(deviceType.get()));
            } else {
                return ResponseEntity.ok(ApiResponse.success("未找到匹配的设备类型", (DeviceType)null));
            }
        } catch (Exception e) {
            log.error("匹配MQTT主题失败: topic={}", topic, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "匹配MQTT主题失败: " + e.getMessage()));
        }
    }

    /**
     * 获取设备类型统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息", description = "获取设备类型的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            DeviceTypeService.DeviceTypeStatistics stats = deviceTypeService.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", stats.getTotalCount());
            response.put("enabledCount", stats.getEnabledCount());
            response.put("disabledCount", stats.getDisabledCount());
            response.put("enabledRate", stats.getTotalCount() > 0 ?
                        (double) stats.getEnabledCount() / stats.getTotalCount() * 100 : 0.0);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取设备类型统计信息失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 初始化默认设备类型
     */
    @PostMapping("/initialize")
    @Operation(summary = "初始化默认设备类型", description = "创建系统默认的设备类型")
    public ResponseEntity<ApiResponse<String>> initializeDefaultDeviceTypes() {
        try {
            deviceTypeService.initializeDefaultDeviceTypes();
            return ResponseEntity.ok(ApiResponse.success("默认设备类型初始化完成"));
        } catch (Exception e) {
            log.error("初始化默认设备类型失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "初始化失败: " + e.getMessage()));
        }
    }
}