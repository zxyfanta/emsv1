package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.response.AlertResponse;
import com.cdutetc.ems.entity.Alert;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.service.AlertService;
import com.cdutetc.ems.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * 获取告警列表（分页）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AlertResponse>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean resolved) {

        User currentUser = getCurrentUser();
        Pageable pageable = Pageable.ofSize(size).withPage(page);

        Page<Alert> alerts;
        if (resolved != null) {
            // 按解决状态过滤（暂未实现，可在AlertRepository中添加）
            alerts = alertService.getAlerts(currentUser.getCompany().getId(), pageable);
        } else {
            alerts = alertService.getAlerts(currentUser.getCompany().getId(), pageable);
        }

        Page<AlertResponse> responses = alerts.map(AlertResponse::fromAlert);
        return ResponseEntity.ok(ApiResponse.success("获取告警列表成功", responses));
    }

    /**
     * 获取未解决的告警
     */
    @GetMapping("/unresolved")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getUnresolvedAlerts() {
        User currentUser = getCurrentUser();
        List<Alert> alerts = alertService.getUnresolvedAlerts(currentUser.getCompany().getId());

        List<AlertResponse> responses = alerts.stream()
                .map(AlertResponse::fromAlert)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取未解决告警成功", responses));
    }

    /**
     * 获取最近的告警
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getRecentAlerts(
            @RequestParam(defaultValue = "10") int limit) {

        User currentUser = getCurrentUser();
        List<Alert> alerts = alertService.getRecentAlerts(currentUser.getCompany().getId(), limit);

        List<AlertResponse> responses = alerts.stream()
                .map(AlertResponse::fromAlert)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取最近告警成功", responses));
    }

    /**
     * 按类型获取告警
     */
    @GetMapping("/type/{alertType}")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsByType(
            @PathVariable String alertType) {

        User currentUser = getCurrentUser();
        List<Alert> alerts = alertService.getAlertsByType(
                currentUser.getCompany().getId(),
                alertType.toUpperCase()
        );

        List<AlertResponse> responses = alerts.stream()
                .map(AlertResponse::fromAlert)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取告警成功", responses));
    }

    /**
     * 解决告警
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Alert alert = alertService.resolveAlert(id, currentUser.getCompany().getId());

        return ResponseEntity.ok(ApiResponse.success(
                "告警已解决",
                AlertResponse.fromAlert(alert)
        ));
    }

    /**
     * 批量解决设备的告警
     */
    @PostMapping("/device/{deviceId}/resolve-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveDeviceAlerts(
            @PathVariable Long deviceId) {

        User currentUser = getCurrentUser();
        int count = alertService.resolveAlertsByDevice(deviceId, currentUser.getCompany().getId());

        return ResponseEntity.ok(ApiResponse.success(
                "批量解决告警成功",
                Map.of("count", count, "deviceId", deviceId)
        ));
    }

    /**
     * 获取告警统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        User currentUser = getCurrentUser();

        long totalUnresolved = alertService.countUnresolvedAlerts(currentUser.getCompany().getId());
        Map<String, Long> severityStats = alertService.getAlertStatistics(currentUser.getCompany().getId());

        return ResponseEntity.ok(ApiResponse.success("获取统计信息成功", Map.of(
                "totalUnresolved", totalUnresolved,
                "bySeverity", severityStats
        )));
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}
