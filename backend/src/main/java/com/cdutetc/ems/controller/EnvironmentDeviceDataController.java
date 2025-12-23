package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.response.EnvironmentDeviceDataResponse;
import com.cdutetc.ems.dto.response.PageResponse;
import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.service.EnvironmentDeviceDataService;
import com.cdutetc.ems.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 环境监测站数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/environment-data")
@RequiredArgsConstructor
public class EnvironmentDeviceDataController {

    private final EnvironmentDeviceDataService environmentDeviceDataService;

    /**
     * 获取环境监测数据列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EnvironmentDeviceDataResponse>>> getEnvironmentData(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<EnvironmentDeviceData> data;
            if (deviceCode != null && !deviceCode.isEmpty()) {
                // 验证设备属于当前用户的企业
                environmentDeviceDataService.validateDeviceAccess(deviceCode, currentUser.getCompany().getId());
                if (startTime != null && endTime != null) {
                    data = environmentDeviceDataService.getDataByDeviceCodeAndTimeRange(
                            deviceCode, startTime, endTime, pageable);
                } else {
                    data = environmentDeviceDataService.getDataByDeviceCode(deviceCode, pageable);
                }
            } else {
                if (startTime != null && endTime != null) {
                    data = environmentDeviceDataService.getDataByTimeRange(
                            currentUser.getCompany().getId(), startTime, endTime, pageable);
                } else {
                    data = environmentDeviceDataService.getData(currentUser.getCompany().getId(), pageable);
                }
            }

            PageResponse<EnvironmentDeviceDataResponse> response = PageResponse.<EnvironmentDeviceDataResponse>builder()
                    .content(data.getContent().stream()
                            .map(EnvironmentDeviceDataResponse::fromEnvironmentDeviceData)
                            .toList())
                    .page(data.getNumber())
                    .size(data.getSize())
                    .totalElements(data.getTotalElements())
                    .totalPages(data.getTotalPages())
                    .first(data.isFirst())
                    .last(data.isLast())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("获取环境监测数据成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Access denied for environment data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting environment data: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取环境监测数据失败，请稍后重试"));
        }
    }

    /**
     * 获取环境监测数据详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EnvironmentDeviceDataResponse>> getEnvironmentDataDetail(@PathVariable Long id) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            EnvironmentDeviceData data = environmentDeviceDataService.getDataDetail(id, currentUser.getCompany().getId());
            EnvironmentDeviceDataResponse response = EnvironmentDeviceDataResponse.fromEnvironmentDeviceData(data);

            return ResponseEntity.ok(ApiResponse.success("获取环境监测数据详情成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Environment data not found: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting environment data detail: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取环境监测数据详情失败，请稍后重试"));
        }
    }

    /**
     * 获取最新的环境监测数据
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<EnvironmentDeviceDataResponse>> getLatestEnvironmentData(
            @RequestParam(required = false) String deviceCode) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            EnvironmentDeviceData data;
            if (deviceCode != null && !deviceCode.isEmpty()) {
                // 验证设备属于当前用户的企业
                environmentDeviceDataService.validateDeviceAccess(deviceCode, currentUser.getCompany().getId());
                data = environmentDeviceDataService.getLatestDataByDeviceCode(deviceCode);
            } else {
                data = environmentDeviceDataService.getLatestData(currentUser.getCompany().getId());
            }

            if (data == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.notFound("未找到环境监测数据"));
            }

            EnvironmentDeviceDataResponse response = EnvironmentDeviceDataResponse.fromEnvironmentDeviceData(data);
            return ResponseEntity.ok(ApiResponse.success("获取最新环境监测数据成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Access denied for latest environment data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting latest environment data: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取最新环境监测数据失败，请稍后重试"));
        }
    }

    /**
     * 获取环境数据统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getEnvironmentDataStatistics(
            @RequestParam(required = false) String deviceCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            Object statistics;
            if (deviceCode != null && !deviceCode.isEmpty()) {
                // 验证设备属于当前用户的企业
                environmentDeviceDataService.validateDeviceAccess(deviceCode, currentUser.getCompany().getId());
                statistics = environmentDeviceDataService.getStatisticsByDeviceCode(deviceCode, startTime, endTime);
            } else {
                statistics = environmentDeviceDataService.getStatistics(currentUser.getCompany().getId(), startTime, endTime);
            }

            return ResponseEntity.ok(ApiResponse.success("获取环境数据统计信息成功", statistics));

        } catch (IllegalArgumentException e) {
            log.warn("Access denied for environment data statistics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting environment data statistics: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取环境数据统计信息失败，请稍后重试"));
        }
    }
}