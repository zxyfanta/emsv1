package com.ems.controller.system;

import com.ems.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author EMS Team
 */
@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "系统健康检查相关接口")
public class HealthController {

    /**
     * 系统健康检查
     */
    @GetMapping
    @Operation(summary = "健康检查", description = "检查系统运行状态")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());

        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("application", "EMS Backend");
        systemInfo.put("version", "1.0.0");
        systemInfo.put("description", "企业级MQTT设备管理云平台");
        systemInfo.put("java.version", System.getProperty("java.version"));
        systemInfo.put("spring.boot.version", "3.5.0");
        systemInfo.put("build.time", LocalDateTime.now());

        return ApiResponse.success(systemInfo);
    }

    /**
     * 简单健康检查（用于负载均衡器）
     */
    @GetMapping("/simple")
    @Operation(summary = "简单健康检查", description = "用于负载均衡器的简单健康检查")
    public ApiResponse<String> simpleHealth() {
        return ApiResponse.success("UP");
    }
}