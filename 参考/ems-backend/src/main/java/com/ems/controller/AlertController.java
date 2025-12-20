package com.ems.controller;

import com.ems.dto.common.ApiResponse;
import com.ems.dto.common.PageResponse;
import com.ems.entity.AlertRecord;
import com.ems.entity.AlertRule;
import com.ems.entity.device.Device;
import com.ems.repository.AlertRecordRepository;
import com.ems.repository.AlertRuleRepository;
import com.ems.service.AlertService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警管理控制器
 * 提供告警记录查询、告警规则管理、告警操作等API
 *
 * @author EMS Team
 */
@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警管理相关接口")
public class AlertController {

    private final AlertService alertService;
    private final AlertRecordRepository alertRecordRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final DeviceService deviceService;

    /**
     * 获取告警记录列表
     */
    @GetMapping("/records")
    @Operation(summary = "获取告警记录", description = "分页获取告警记录列表")
    public ResponseEntity<ApiResponse<Page<AlertRecord>>> getAlertRecords(
            @Parameter(description = "设备ID") @RequestParam(required = false) String deviceId,
            @Parameter(description = "告警状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            // 权限检查
            String userRole = SecurityUtils.getCurrentUserRole();
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();

            // 构建分页和排序
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "triggeredAt"));

            Page<AlertRecord> records;

            if (deviceId != null && !deviceId.isEmpty()) {
                // 查询特定设备的告警记录
                records = alertRecordRepository.findByDeviceId(deviceId, pageable);
            } else if (status != null && !status.isEmpty()) {
                // 按状态查询
                try {
                    AlertRecord.AlertStatus alertStatus = AlertRecord.AlertStatus.valueOf(status.toUpperCase());
                    List<AlertRecord> statusAlerts = alertRecordRepository.findByStatus(alertStatus);
                    // 手动创建分页对象
                    int start = page * size;
                    int end = Math.min(start + size, statusAlerts.size());
                    List<AlertRecord> pageContent = statusAlerts.subList(start, end);
                    records = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, statusAlerts.size());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.<Page<AlertRecord>>error("无效的告警状态: " + status));
                }
            } else {
                // 查询所有告警记录
                records = alertRecordRepository.findAll(pageable);
            }

            log.info("查询告警记录: deviceId={}, status={}, 返回{}条", deviceId, status, records.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(records));

        } catch (Exception e) {
            log.error("获取告警记录失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Page<AlertRecord>>error("获取告警记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取活跃告警列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃告警", description = "获取当前活跃的告警列表")
    public ResponseEntity<ApiResponse<List<AlertRecord>>> getActiveAlerts() {
        try {
            List<AlertRecord> activeAlerts = alertService.getActiveAlerts();
            log.info("获取活跃告警: 返回{}条", activeAlerts.size());
            return ResponseEntity.ok(ApiResponse.success(activeAlerts));

        } catch (Exception e) {
            log.error("获取活跃告警失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<AlertRecord>>error("获取活跃告警失败: " + e.getMessage()));
        }
    }

    /**
     * 获取设备告警记录
     */
    @GetMapping("/records/device/{deviceId}")
    @Operation(summary = "获取设备告警记录", description = "获取指定设备的告警记录列表")
    public ResponseEntity<ApiResponse<List<AlertRecord>>> getDeviceAlertRecords(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "50") int size) {

        try {
            // 权限检查
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();
            Device device = deviceService.findByDeviceId(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + deviceId));

            // 企业权限检查
            if (!SecurityUtils.isPlatformAdmin() && userEnterpriseId != null) {
                Long deviceEnterpriseId = device.getEnterprise().getId();
                if (!deviceEnterpriseId.equals(userEnterpriseId)) {
                    return ResponseEntity.ok(ApiResponse.error(403, "无权限访问该设备"));
                }
            }

            List<AlertRecord> alertRecords = alertService.getDeviceAlertRecords(deviceId);
            log.info("获取设备告警记录: 设备={}, 返回{}条", deviceId, alertRecords.size());
            return ResponseEntity.ok(ApiResponse.success(alertRecords));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<AlertRecord>>error(e.getMessage()));
        } catch (Exception e) {
            log.error("获取设备告警记录失败: 设备={}", deviceId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<AlertRecord>>error("获取设备告警记录失败: " + e.getMessage()));
        }
    }

    /**
     * 确认告警
     */
    @PutMapping("/records/{id}/acknowledge")
    @Operation(summary = "确认告警", description = "确认指定的告警记录")
    public ResponseEntity<ApiResponse<String>> acknowledgeAlert(
            @Parameter(description = "告警ID") @PathVariable Long id,
            @Parameter(description = "确认备注") @RequestParam(required = false) String notes) {

        try {
            String acknowledgedBy = SecurityUtils.getCurrentUsername();
            alertService.acknowledgeAlert(id, acknowledgedBy, notes);
            log.info("告警确认成功: ID={}, 确认人={}", id, acknowledgedBy);
            return ResponseEntity.ok(ApiResponse.success("告警确认成功"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error(e.getMessage()));
        } catch (Exception e) {
            log.error("确认告警失败: ID={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error("确认告警失败: " + e.getMessage()));
        }
    }

    /**
     * 解决告警
     */
    @PutMapping("/records/{id}/resolve")
    @Operation(summary = "解决告警", description = "解决指定的告警记录")
    public ResponseEntity<ApiResponse<String>> resolveAlert(
            @Parameter(description = "告警ID") @PathVariable Long id,
            @Parameter(description = "解决备注") @RequestParam(required = false) String notes) {

        try {
            alertService.resolveAlert(id, notes);
            log.info("告警解决成功: ID={}", id);
            return ResponseEntity.ok(ApiResponse.success("告警解决成功"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error(e.getMessage()));
        } catch (Exception e) {
            log.error("解决告警失败: ID={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error("解决告警失败: " + e.getMessage()));
        }
    }

    /**
     * 动态处理告警
     * 支持前端使用的动态action参数处理方式
     */
    @PutMapping("/records/{alertId}/{action}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN')")
    @Operation(summary = "动态处理告警", description = "根据action参数动态处理告警")
    public ResponseEntity<ApiResponse<String>> handleAlert(
            @Parameter(description = "告警ID") @PathVariable Long alertId,
            @Parameter(description = "处理动作") @PathVariable String action,
            @RequestBody(required = false) Map<String, Object> params) {

        try {
            String username = SecurityUtils.getCurrentUsername();

            switch (action.toLowerCase()) {
                case "acknowledge":
                    String notes = params != null ? (String) params.get("notes") : null;
                    alertService.acknowledgeAlert(alertId, username, notes);
                    log.info("告警确认成功: ID={}, 确认人={}", alertId, username);
                    return ResponseEntity.ok(ApiResponse.success("告警确认成功"));

                case "resolve":
                    String resolveNotes = params != null ? (String) params.get("notes") : null;
                    alertService.resolveAlert(alertId, resolveNotes);
                    log.info("告警解决成功: ID={}", alertId);
                    return ResponseEntity.ok(ApiResponse.success("告警解决成功"));

                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.<String>error("不支持的告警处理操作: " + action));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error(e.getMessage()));
        } catch (Exception e) {
            log.error("处理告警失败: ID={}, action={}", alertId, action, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error("处理告警失败: " + e.getMessage()));
        }
    }

    /**
     * 获取告警统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取告警统计", description = "获取告警统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 总告警数量
            long totalAlerts = alertRecordRepository.count();

            // 按状态统计
            List<AlertRecord> activeAlerts = alertService.getActiveAlerts();
            long activeCount = activeAlerts.size();

            // 按严重级别统计
            List<Object[]> severityStats = alertRecordRepository.countAlertsBySeverity();
            Map<String, Long> severityCount = new HashMap<>();
            if (severityStats != null) {
                for (Object[] stat : severityStats) {
                    severityCount.put(stat[0].toString(), (Long) stat[1]);
                }
            }

            // 最近24小时告警数量
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<AlertRecord> recentAlerts = alertRecordRepository.findByTriggeredAtBetween(yesterday, LocalDateTime.now());
            long recentCount = recentAlerts.size();

            // 告警率计算
            double resolutionRate = totalAlerts > 0 ?
                    (double) alertRecordRepository.findByTriggeredAtBetween(LocalDateTime.now().minusDays(7), LocalDateTime.now()).stream()
                            .filter(alert -> alert.isResolved())
                            .count() / Math.min(totalAlerts, 1000) * 100 : 0;

            statistics.put("totalAlerts", totalAlerts);
            statistics.put("activeCount", activeCount);
            statistics.put("severityCount", severityCount);
            statistics.put("recentCount", recentCount);
            statistics.put("resolutionRate", Math.round(resolutionRate * 100) / 100.0);

            log.info("获取告警统计完成");
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("获取告警统计失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error("获取告警统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    @Operation(summary = "获取告警规则", description = "获取告警规则列表")
    public ResponseEntity<ApiResponse<List<AlertRule>>> getAlertRules(
            @Parameter(description = "设备ID") @RequestParam(required = false) String deviceId) {

        try {
            List<AlertRule> rules;

            if (deviceId != null && !deviceId.isEmpty()) {
                // 查询特定设备的告警规则
                rules = alertService.getDeviceAlertRules(deviceId);
            } else {
                // 查询所有启用的告警规则
                rules = alertRuleRepository.findEnabledRules();
            }

            log.info("获取告警规则: deviceId={}, 返回{}条", deviceId, rules.size());
            return ResponseEntity.ok(ApiResponse.success(rules));

        } catch (Exception e) {
            log.error("获取告警规则失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<AlertRule>>error("获取告警规则失败: " + e.getMessage()));
        }
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    @Operation(summary = "创建告警规则", description = "创建新的告警规则")
    public ResponseEntity<ApiResponse<AlertRule>> createAlertRule(
            @Valid @RequestBody AlertRule alertRule) {

        try {
            // 权限检查
            Long userEnterpriseId = SecurityUtils.getCurrentUserEnterpriseId();
            if (!SecurityUtils.isPlatformAdmin() && userEnterpriseId != null) {
                // 企业用户只能为自己企业创建规则
                Device device = deviceService.findByDeviceId(alertRule.getDevice().getDeviceId())
                        .orElseThrow(() -> new IllegalArgumentException("设备不存在"));

                if (!device.getEnterprise().getId().equals(userEnterpriseId)) {
                    return ResponseEntity.ok(ApiResponse.error(403, "无权限为该设备创建告警规则"));
                }
            }

            AlertRule createdRule = alertService.createAlertRule(alertRule);
            log.info("创建告警规则成功: 设备={}, 规则={}",
                    createdRule.getDevice().getDeviceId(), createdRule.getRuleName());
            return ResponseEntity.ok(ApiResponse.success("告警规则创建成功", createdRule));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AlertRule>error(e.getMessage()));
        } catch (Exception e) {
            log.error("创建告警规则失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AlertRule>error("创建告警规则失败: " + e.getMessage()));
        }
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rules/{id}")
    @Operation(summary = "更新告警规则", description = "更新指定的告警规则")
    public ResponseEntity<ApiResponse<AlertRule>> updateAlertRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody AlertRule alertRule) {

        try {
            AlertRule updatedRule = alertService.updateAlertRule(id, alertRule);
            log.info("更新告警规则成功: ID={}", id);
            return ResponseEntity.ok(ApiResponse.success("告警规则更新成功", updatedRule));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AlertRule>error(e.getMessage()));
        } catch (Exception e) {
            log.error("更新告警规则失败: ID={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AlertRule>error("更新告警规则失败: " + e.getMessage()));
        }
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除告警规则", description = "删除指定的告警规则")
    public ResponseEntity<ApiResponse<String>> deleteAlertRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {

        try {
            alertService.deleteAlertRule(id);
            log.info("删除告警规则成功: ID={}", id);
            return ResponseEntity.ok(ApiResponse.success("告警规则删除成功"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error(e.getMessage()));
        } catch (Exception e) {
            log.error("删除告警规则失败: ID={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error("删除告警规则失败: " + e.getMessage()));
        }
    }

    /**
     * 启用/禁用告警规则
     */
    @PutMapping("/rules/{id}/enable")
    @Operation(summary = "启用/禁用告警规则", description = "启用或禁用指定的告警规则")
    public ResponseEntity<ApiResponse<String>> toggleAlertRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {

        try {
            AlertRule rule = alertRuleRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));

            rule.setEnabled(enabled);
            alertRuleRepository.save(rule);

            String action = enabled ? "启用" : "禁用";
            log.info("{}告警规则成功: ID={}, 规则={}", action, id, rule.getRuleName());
            return ResponseEntity.ok(ApiResponse.success("告警规则" + action + "成功"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error(e.getMessage()));
        } catch (Exception e) {
            log.error("启用/禁用告警规则失败: ID={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error("启用/禁用告警规则失败: " + e.getMessage()));
        }
    }
}