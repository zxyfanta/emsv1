package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.response.PageResponse;
import com.cdutetc.ems.util.ApiResponse;
import com.cdutetc.ems.entity.BackupLog;
import com.cdutetc.ems.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据备份管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 立即执行全量备份
     */
    @PostMapping("/execute/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> executeFullBackup() {
        log.info("手动触发全量备份");

        List<BackupLog> results = backupService.backupAll("MANUAL");

        Map<String, Object> data = new HashMap<>();
        data.put("total", results.size());
        data.put("results", results);

        return ApiResponse.success(data);
    }

    /**
     * 备份时序数据
     */
    @PostMapping("/execute/timeseries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BackupLog> backupTimeseries() {
        log.info("手动触发时序数据备份");
        BackupLog result = backupService.backupTimeseriesData("MANUAL");
        return ApiResponse.success(result);
    }

    /**
     * 备份业务数据
     */
    @PostMapping("/execute/business")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BackupLog> backupBusiness() {
        log.info("手动触发业务数据备份");
        BackupLog result = backupService.backupBusinessData("MANUAL");
        return ApiResponse.success(result);
    }

    /**
     * 备份系统配置数据
     */
    @PostMapping("/execute/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BackupLog> backupSystem() {
        log.info("手动触发系统配置数据备份");
        BackupLog result = backupService.backupSystemData("MANUAL");
        return ApiResponse.success(result);
    }

    /**
     * 查询备份日志列表
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<BackupLog>> getBackupLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        // TODO: 实现分页查询
        PageResponse<BackupLog> response = PageResponse.<BackupLog>builder()
            .content(backupService.getRecentBackups())
            .page(0)
            .size(10)
            .totalElements(10)
            .totalPages(1)
            .build();

        return ApiResponse.success(response);
    }

    /**
     * 获取最近的备份记录
     */
    @GetMapping("/logs/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<BackupLog>> getRecentBackups() {
        List<BackupLog> logs = backupService.getRecentBackups();
        return ApiResponse.success(logs);
    }

    /**
     * 获取备份统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = backupService.getStatistics();
        return ApiResponse.success(stats);
    }
}
