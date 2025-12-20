package com.ems.controller.device;

import com.ems.dto.common.ApiResponse;
import com.ems.dto.common.PageResponse;
import com.ems.dto.device.*;
import com.ems.entity.device.Device;
import com.ems.service.device.DeviceService;
import com.ems.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 设备管理控制器
 * 提供设备查询、状态管理、数据查询等API
 *
 * @author EMS Team
 */
@Slf4j
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备管理相关接口")
public class DeviceController {

    private final DeviceService deviceService;

  
    /**
     * 获取设备列表
     */
    @GetMapping
    @Operation(summary = "获取设备列表", description = "分页获取所有设备")
    public ResponseEntity<ApiResponse<Page<Device>>> getDevices(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Device> devices = deviceService.findAllDevices(pageable);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * 根据设备ID查询设备
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "查询设备", description = "根据设备ID查询设备详细信息")
    public ResponseEntity<ApiResponse<Device>> getDevice(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {

        Optional<Device> device = deviceService.findByDeviceId(deviceId);
        return device.map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                .orElse(ResponseEntity.ok(ApiResponse.error(404, "设备不存在")));
    }

    /**
     * 获取在线设备列表
     */
    @GetMapping("/online")
    @Operation(summary = "获取在线设备", description = "获取所有在线设备列表")
    public ResponseEntity<ApiResponse<List<Device>>> getOnlineDevices() {
        List<Device> devices = deviceService.findOnlineDevices();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * 获取离线设备列表
     */
    @GetMapping("/offline")
    @Operation(summary = "获取离线设备", description = "获取所有离线设备列表")
    public ResponseEntity<ApiResponse<List<Device>>> getOfflineDevices() {
        List<Device> devices = deviceService.findOfflineDevices();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * 根据设备类型查询设备
     */
    @GetMapping("/type/{deviceType}")
    @Operation(summary = "按类型查询设备", description = "根据设备类型查询设备")
    public ResponseEntity<ApiResponse<List<Device>>> getDevicesByType(
            @Parameter(description = "设备类型") @PathVariable String deviceType) {

        List<Device> devices = deviceService.findByDeviceType(deviceType);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * 更新设备状态
     */
    @PutMapping("/{deviceId}/status")
    @Operation(summary = "更新设备状态", description = "更新设备在线/离线状态")
    public ResponseEntity<ApiResponse<String>> updateDeviceStatus(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "设备状态") @RequestParam Device.DeviceStatus status) {

        boolean updated = deviceService.updateDeviceStatus(deviceId, status);
        if (updated) {
            return ResponseEntity.ok(ApiResponse.success("设备状态更新成功"));
        } else {
            return ResponseEntity.ok(ApiResponse.error(404, "设备不存在"));
        }
    }

    /**
     * 获取设备统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "设备统计", description = "获取设备数量统计信息")
    public ResponseEntity<ApiResponse<DeviceService.DeviceStats>> getDeviceStats() {
        DeviceService.DeviceStats stats = deviceService.getDeviceStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取智能设备统计数据（基于Redis实时数据）
     */
    @GetMapping("/smart-stats")
    @Operation(summary = "智能设备统计", description = "基于Redis实时数据获取设备在线统计信息")
    public ResponseEntity<ApiResponse<com.ems.service.DeviceOnlineStatusService.DeviceOnlineStats>> getSmartDeviceStats() {
        com.ems.service.DeviceOnlineStatusService.DeviceOnlineStats stats = deviceService.getSmartDeviceStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取设备实时在线状态
     */
    @GetMapping("/{deviceId}/realtime-status")
    @Operation(summary = "获取设备实时在线状态", description = "基于Redis实时数据判断设备在线状态")
    public ResponseEntity<ApiResponse<com.ems.service.DeviceOnlineStatusService.DeviceOnlineStatus>> getDeviceRealTimeStatus(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        try {
            com.ems.service.DeviceOnlineStatusService.DeviceOnlineStatus status =
                deviceService.getDeviceOnlineStatus(deviceId);
            return ResponseEntity.ok(ApiResponse.success("获取设备实时状态成功", status));
        } catch (Exception e) {
            log.error("获取设备实时状态失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<com.ems.service.DeviceOnlineStatusService.DeviceOnlineStatus>error("获取设备实时状态失败: " + e.getMessage()));
        }
    }

    /**
     * 并行验证接口 - 对比Redis方案和数据库方案
     */
    @GetMapping("/{deviceId}/parallel-status")
    @Operation(summary = "并行状态验证", description = "对比Redis实时数据和数据库字段的状态判断结果")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getParallelStatus(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        try {
            java.util.Map<String, Object> comparison = new java.util.HashMap<>();

            // 传统方案（数据库字段）
            Device.DeviceStatus dbStatus = deviceService.findByDeviceId(deviceId)
                .map(Device::getStatus)
                .orElse(Device.DeviceStatus.OFFLINE);
            comparison.put("database_status", dbStatus);
            comparison.put("database_status_desc", dbStatus.getDescription());

            // Redis方案（实时数据）
            com.ems.service.DeviceOnlineStatusService.DeviceOnlineStatus redisStatus =
                deviceService.getDeviceOnlineStatus(deviceId);
            comparison.put("redis_status", redisStatus);
            comparison.put("redis_status_desc", redisStatus.getStatus().getDescription());
            comparison.put("redis_status_color", redisStatus.getStatus().getColor());

            if (redisStatus.getLastDataTime() != null) {
                comparison.put("last_data_time", redisStatus.getLastDataTime());
                comparison.put("offline_duration", redisStatus.getOfflineDurationText());
            }

            // 数据一致性检查
            boolean isConsistent = dbStatus.equals(redisStatus.getDeviceStatus());
            comparison.put("data_consistency", isConsistent);
            comparison.put("consistency_desc", isConsistent ? "一致" : "不一致");

            // 状态源信息
            comparison.put("status_source", "Redis实时数据优先，数据库字段回退");
            comparison.put("online_threshold_minutes", 5);

            return ResponseEntity.ok(ApiResponse.success("并行状态验证完成", comparison));

        } catch (Exception e) {
            log.error("并行状态验证失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<java.util.Map<String, Object>>error("并行状态验证失败: " + e.getMessage()));
        }
    }

    
    /**
     * 创建设备
     */
    @PostMapping
    @Operation(summary = "创建设备", description = "创建新的设备")
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(
            @Valid @RequestBody DeviceCreateRequest request) {
        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();
            Long targetEnterpriseId = request.getEnterpriseId();

            if (!SecurityUtils.isPlatformAdmin() && userEnterpriseId != null) {
                // 企业用户只能创建自己企业的设备
                targetEnterpriseId = userEnterpriseId;
            }

            Device device = deviceService.createDevice(request, targetEnterpriseId);
            DeviceResponse response = DeviceResponse.fromEntity(device);
            return ResponseEntity.ok(ApiResponse.success("设备创建成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<DeviceResponse>error(e.getMessage()));
        }
    }

    /**
     * 更新设备
     */
    @PutMapping("/{deviceId}")
    @Operation(summary = "更新设备", description = "更新设备信息")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();
            Optional<Device> updatedDevice = deviceService.updateDevice(deviceId, request, userEnterpriseId);

            if (updatedDevice.isPresent()) {
                DeviceResponse response = DeviceResponse.fromEntity(updatedDevice.get());
                return ResponseEntity.ok(ApiResponse.success("设备更新成功", response));
            } else {
                return ResponseEntity.ok(ApiResponse.error(404, "设备不存在"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<DeviceResponse>error(e.getMessage()));
        }
    }

    /**
     * 搜索设备 - 支持多条件筛选
     */
    @GetMapping("/search")
    @Operation(summary = "搜索设备", description = "支持关键词、状态、企业等多条件筛选搜索设备")
    public ResponseEntity<ApiResponse<?>> searchDevices(
            @Parameter(description = "搜索关键词（设备名称或设备ID）") @RequestParam(required = false) String keyword,
            @Parameter(description = "设备状态") @RequestParam(required = false) String status,
            @Parameter(description = "企业ID") @RequestParam(required = false) Long enterpriseId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "是否分页") @RequestParam(defaultValue = "true") boolean paginated) {

        try {
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            // 权限控制：非平台管理员只能查看自己企业的设备
            if (!SecurityUtils.isPlatformAdmin() && userEnterpriseId != null) {
                // 如果用户不是平台管理员，强制设置enterpriseId为用户所属企业
                enterpriseId = userEnterpriseId;
            }

            log.info("搜索设备 - keyword: {}, status: {}, enterpriseId: {}, userRole: {}, userEnterpriseId: {}",
                    keyword, status, enterpriseId, userRole, userEnterpriseId);

            if (paginated) {
                // 分页搜索
                Page<Device> devices = deviceService.searchDevices(
                    keyword, status, userRole, enterpriseId, page, size, sortBy, sortDir);

                // 转换为响应DTO
                List<DeviceResponse> deviceResponses = devices.getContent().stream()
                        .map(DeviceResponse::fromEntity)
                        .collect(Collectors.toList());

                PageResponse<DeviceResponse> response = new PageResponse<>(
                    deviceResponses,
                    devices.getNumber(),
                    devices.getSize(),
                    devices.getTotalElements(),
                    devices.getTotalPages()
                );

                return ResponseEntity.ok(ApiResponse.success("搜索成功", response));
            } else {
                // 不分页搜索（用于导出等场景）
                List<Device> devices = deviceService.searchDevices(keyword, status, userRole, enterpriseId);
                List<DeviceResponse> responses = devices.stream()
                        .map(DeviceResponse::fromEntity)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(ApiResponse.success("搜索成功", responses));
            }
        } catch (Exception e) {
            log.error("搜索设备失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{deviceId}")
    @Operation(summary = "删除设备", description = "软删除设备（标记为已删除）")
    public ResponseEntity<ApiResponse<String>> deleteDevice(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();
            boolean deleted = deviceService.deleteDevice(deviceId, userEnterpriseId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("设备删除成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(404, "设备不存在"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error(e.getMessage()));
        }
    }

    /**
     * 获取设备详细信息
     */
    @GetMapping("/{deviceId}/detail")
    @Operation(summary = "获取设备详细信息", description = "根据设备ID获取设备详细信息，包括规格信息、实时数据等")
    public ResponseEntity<ApiResponse<DeviceDetailResponse>> getDeviceDetail(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            Optional<DeviceDetailResponse> deviceDetail = deviceService.getDeviceDetail(deviceId, userEnterpriseId);

            if (deviceDetail.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("获取设备详情成功", deviceDetail.get()));
            } else {
                return ResponseEntity.ok(ApiResponse.error(404, "设备不存在或无权限访问"));
            }
        } catch (Exception e) {
            log.error("获取设备详情失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<DeviceDetailResponse>error("获取设备详情失败: " + e.getMessage()));
        }
    }

    /**
     * 获取设备历史记录
     */
    @GetMapping("/{deviceId}/history")
    @Operation(summary = "获取设备历史记录", description = "获取设备的历史记录，支持分页查询")
    public ResponseEntity<ApiResponse<PageResponse<DeviceHistoryRecord>>> getDeviceHistory(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "记录类型") @RequestParam(required = false) String recordType) {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            org.springframework.data.domain.Page<DeviceHistoryRecord> historyPage = deviceService.getDeviceHistory(deviceId, userEnterpriseId, page, size);

            // 转换为响应格式
            List<DeviceHistoryRecord> content = historyPage.getContent();
            PageResponse<DeviceHistoryRecord> response = new PageResponse<>(
                content,
                historyPage.getNumber(),
                historyPage.getSize(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages()
            );

            return ResponseEntity.ok(ApiResponse.success("获取历史记录成功", response));
        } catch (Exception e) {
            log.error("获取设备历史记录失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<PageResponse<DeviceHistoryRecord>>error("获取历史记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取设备告警信息
     */
    @GetMapping("/{deviceId}/alerts")
    @Operation(summary = "获取设备告警信息", description = "获取设备的告警信息列表")
    public ResponseEntity<ApiResponse<List<DeviceAlert>>> getDeviceAlerts(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            List<DeviceAlert> alerts = deviceService.getDeviceAlerts(deviceId, userEnterpriseId);

            return ResponseEntity.ok(ApiResponse.success("获取告警信息成功", alerts));
        } catch (Exception e) {
            log.error("获取设备告警信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<DeviceAlert>>error("获取告警信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取设备实时数据
     */
    @GetMapping("/{deviceId}/realtime")
    @Operation(summary = "获取设备实时数据", description = "获取设备的实时监测数据")
    public ResponseEntity<ApiResponse<DeviceRealTimeData>> getDeviceRealTimeData(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        try {
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            Optional<DeviceRealTimeData> realTimeData = deviceService.getDeviceRealTimeData(deviceId, userEnterpriseId);

            if (realTimeData.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("获取实时数据成功", realTimeData.get()));
            } else {
                return ResponseEntity.ok(ApiResponse.error(404, "设备不存在或无权限访问"));
            }
        } catch (Exception e) {
            log.error("获取设备实时数据失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<DeviceRealTimeData>error("获取实时数据失败: " + e.getMessage()));
        }
    }
}