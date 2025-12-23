package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.request.DeviceCreateRequest;
import com.cdutetc.ems.dto.request.DeviceUpdateRequest;
import com.cdutetc.ems.dto.response.DeviceResponse;
import com.cdutetc.ems.dto.response.PageResponse;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.service.DeviceService;
import com.cdutetc.ems.util.ApiResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 创建设备
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            Device device = new Device();
            device.setDeviceCode(request.getDeviceCode());
            device.setDeviceName(request.getDeviceName());
            device.setDeviceType(DeviceType.valueOf(request.getDeviceType()));
            device.setDescription(request.getDescription());
            device.setLocation(request.getLocation());

            Device createdDevice = deviceService.createDevice(device, currentUser.getCompany().getId());
            DeviceResponse response = DeviceResponse.fromDevice(createdDevice);

            log.info("Device {} created successfully by user {}", createdDevice.getDeviceCode(), currentUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(response));

        } catch (IllegalArgumentException e) {
            log.warn("Device creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("设备创建失败，请稍后重试"));
        }
    }

    /**
     * 获取设备详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(@PathVariable Long id) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            Device device = deviceService.getDevice(id, currentUser.getCompany().getId());
            DeviceResponse response = DeviceResponse.fromDevice(device);

            return ResponseEntity.ok(ApiResponse.success("获取设备信息成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Device not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取设备信息失败，请稍后重试"));
        }
    }

    /**
     * 更新设备信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceUpdateRequest request) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            Device device = new Device();
            device.setDeviceName(request.getDeviceName());
            device.setDescription(request.getDescription());
            device.setLocation(request.getLocation());

            Device updatedDevice = deviceService.updateDevice(id, device, currentUser.getCompany().getId());
            DeviceResponse response = DeviceResponse.fromDevice(updatedDevice);

            log.info("Device {} updated successfully by user {}", updatedDevice.getDeviceCode(), currentUser.getUsername());
            return ResponseEntity.ok(ApiResponse.updated(response));

        } catch (IllegalArgumentException e) {
            log.warn("Device update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("设备更新失败，请稍后重试"));
        }
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable Long id) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            deviceService.deleteDevice(id, currentUser.getCompany().getId());

            log.info("Device {} deleted successfully by user {}", id, currentUser.getUsername());
            return ResponseEntity.ok(ApiResponse.deleted());

        } catch (IllegalArgumentException e) {
            log.warn("Device deletion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("设备删除失败，请稍后重试"));
        }
    }

    /**
     * 获取设备列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeviceResponse>>> getDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String search) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Device> devices;
            if (deviceType != null && !deviceType.isEmpty()) {
                DeviceType type = DeviceType.valueOf(deviceType.toUpperCase());
                devices = deviceService.getDevicesByType(currentUser.getCompany().getId(), type, pageable);
            } else if (search != null && !search.isEmpty()) {
                devices = deviceService.searchDevices(currentUser.getCompany().getId(), search, pageable);
            } else {
                devices = deviceService.getDevices(currentUser.getCompany().getId(), pageable);
            }

            PageResponse<DeviceResponse> response = PageResponse.<DeviceResponse>builder()
                    .content(devices.getContent().stream()
                            .map(DeviceResponse::fromDevice)
                            .toList())
                    .page(devices.getNumber())
                    .size(devices.getSize())
                    .totalElements(devices.getTotalElements())
                    .totalPages(devices.getTotalPages())
                    .first(devices.isFirst())
                    .last(devices.isLast())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("获取设备列表成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid device type: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting devices: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取设备列表失败，请稍后重试"));
        }
    }

    /**
     * 更新设备状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            DeviceStatus deviceStatus = DeviceStatus.valueOf(status.toUpperCase());
            Device updatedDevice = deviceService.updateDeviceStatus(id, deviceStatus, currentUser.getCompany().getId());
            DeviceResponse response = DeviceResponse.fromDevice(updatedDevice);

            log.info("Device {} status updated to {} by user {}",
                    updatedDevice.getDeviceCode(), deviceStatus, currentUser.getUsername());
            return ResponseEntity.ok(ApiResponse.updated(response));

        } catch (IllegalArgumentException e) {
            log.warn("Device status update failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating device status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("设备状态更新失败，请稍后重试"));
        }
    }

    /**
     * 获取设备统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DeviceService.DeviceStatistics>> getDeviceStatistics() {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            DeviceService.DeviceStatistics statistics = deviceService.getDeviceStatistics(currentUser.getCompany().getId());
            return ResponseEntity.ok(ApiResponse.success("获取设备统计成功", statistics));

        } catch (Exception e) {
            log.error("Error getting device statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取设备统计失败，请稍后重试"));
        }
    }
}