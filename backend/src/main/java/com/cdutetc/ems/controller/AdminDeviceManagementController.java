package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.request.BatchDeviceImportRequest;
import com.cdutetc.ems.dto.response.*;
import com.cdutetc.ems.service.DeviceActivationService;
import com.cdutetc.ems.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/devices")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDeviceManagementController {

    private final DeviceActivationService deviceActivationService;

    /**
     * 批量录入设备（出厂）
     * 设备初始不绑定企业，由用户使用激活码激活时绑定
     */
    @PostMapping("/batch-import")
    public ResponseEntity<ApiResponse<BatchImportResult>> batchImportDevices(
            @Valid @RequestBody BatchDeviceImportRequest request) {

        try {
            BatchImportResult result = deviceActivationService.batchImportDevices(request.getItems());

            log.info("✅ 批量导入完成: 成功 {}/{}", result.getImportedCount(), result.getTotalCount());

            return ResponseEntity.ok(ApiResponse.success("设备导入成功", result));

        } catch (Exception e) {
            log.error("❌ 批量导入失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("设备导入失败: " + e.getMessage()));
        }
    }

    /**
     * 查询待激活设备列表
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingDeviceResponse>>> getPendingDevices() {
        try {
            List<PendingDeviceResponse> devices = deviceActivationService.getPendingDevices();
            return ResponseEntity.ok(ApiResponse.success("查询成功", devices));
        } catch (Exception e) {
            log.error("❌ 查询待激活设备失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 查询已激活设备列表
     */
    @GetMapping("/activated")
    public ResponseEntity<ApiResponse<List<ActivatedDeviceResponse>>> getActivatedDevices() {
        try {
            List<ActivatedDeviceResponse> devices = deviceActivationService.getActivatedDevices();
            return ResponseEntity.ok(ApiResponse.success("查询成功", devices));
        } catch (Exception e) {
            log.error("❌ 查询已激活设备失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
}
