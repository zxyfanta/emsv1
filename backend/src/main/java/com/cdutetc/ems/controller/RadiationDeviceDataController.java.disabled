package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.response.PageResponse;
import com.cdutetc.ems.dto.response.RadiationDeviceDataResponse;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.service.RadiationDeviceDataService;
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
 * 辐射监测仪数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/radiation-data")
@RequiredArgsConstructor
public class RadiationDeviceDataController {

    private final RadiationDeviceDataService radiationDeviceDataService;

    /**
     * 获取辐射监测数据列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RadiationDeviceDataResponse>>> getRadiationData(
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

            Page<RadiationDeviceData> data;
            if (deviceCode != null && !deviceCode.isEmpty()) {
                // 验证设备属于当前用户的企业
                radiationDeviceDataService.validateDeviceAccess(deviceCode, currentUser.getCompany().getId());
                if (startTime != null && endTime != null) {
                    data = radiationDeviceDataService.getDataByDeviceCodeAndTimeRange(
                            deviceCode, startTime, endTime, pageable);
                } else {
                    data = radiationDeviceDataService.getDataByDeviceCode(deviceCode, pageable);
                }
            } else {
                if (startTime != null && endTime != null) {
                    data = radiationDeviceDataService.getDataByTimeRange(
                            currentUser.getCompany().getId(), startTime, endTime, pageable);
                } else {
                    data = radiationDeviceDataService.getData(currentUser.getCompany().getId(), pageable);
                }
            }

            PageResponse<RadiationDeviceDataResponse> response = PageResponse.<RadiationDeviceDataResponse>builder()
                    .content(data.getContent().stream()
                            .map(RadiationDeviceDataResponse::fromRadiationDeviceData)
                            .toList())
                    .page(data.getNumber())
                    .size(data.getSize())
                    .totalElements(data.getTotalElements())
                    .totalPages(data.getTotalPages())
                    .first(data.isFirst())
                    .last(data.isLast())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("获取辐射监测数据成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Access denied for radiation data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting radiation data: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取辐射监测数据失败，请稍后重试"));
        }
    }

    /**
     * 获取辐射监测数据详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RadiationDeviceDataResponse>> getRadiationDataDetail(@PathVariable Long id) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            RadiationDeviceData data = radiationDeviceDataService.getDataDetail(id, currentUser.getCompany().getId());
            RadiationDeviceDataResponse response = RadiationDeviceDataResponse.fromRadiationDeviceData(data);

            return ResponseEntity.ok(ApiResponse.success("获取辐射监测数据详情成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Radiation data not found: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting radiation data detail: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取辐射监测数据详情失败，请稍后重试"));
        }
    }

    /**
     * 获取最新的辐射监测数据
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<RadiationDeviceDataResponse>> getLatestRadiationData(
            @RequestParam(required = false) String deviceCode) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            RadiationDeviceData data;
            if (deviceCode != null && !deviceCode.isEmpty()) {
                // 验证设备属于当前用户的企业
                radiationDeviceDataService.validateDeviceAccess(deviceCode, currentUser.getCompany().getId());
                data = radiationDeviceDataService.getLatestDataByDeviceCode(deviceCode);
            } else {
                data = radiationDeviceDataService.getLatestData(currentUser.getCompany().getId());
            }

            if (data == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.notFound("未找到辐射监测数据"));
            }

            RadiationDeviceDataResponse response = RadiationDeviceDataResponse.fromRadiationDeviceData(data);
            return ResponseEntity.ok(ApiResponse.success("获取最新辐射监测数据成功", response));

        } catch (IllegalArgumentException e) {
            log.warn("Access denied for latest radiation data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting latest radiation data: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取最新辐射监测数据失败，请稍后重试"));
        }
    }

    /**
     * 获取辐射数据统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getRadiationDataStatistics(
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
                radiationDeviceDataService.validateDeviceAccess(deviceCode, currentUser.getCompany().getId());
                statistics = radiationDeviceDataService.getStatisticsByDeviceCode(deviceCode, startTime, endTime);
            } else {
                statistics = radiationDeviceDataService.getStatistics(currentUser.getCompany().getId(), startTime, endTime);
            }

            return ResponseEntity.ok(ApiResponse.success("获取辐射数据统计信息成功", statistics));

        } catch (IllegalArgumentException e) {
            log.warn("Access denied for radiation data statistics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting radiation data statistics: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取辐射数据统计信息失败，请稍后重试"));
        }
    }
}