package com.cdutetc.ems.controller;

import com.cdutetc.ems.config.FeatureConfig;
import com.cdutetc.ems.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置控制器
 * 提供系统功能开关配置查询接口
 */
@Slf4j
@RestController
@RequestMapping("/system-config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final FeatureConfig featureConfig;

    /**
     * 获取系统配置
     * 返回当前启用的功能模块，前端根据此配置动态显示/隐藏菜单
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("radiationEnabled", featureConfig.getRadiationEnabled());
        config.put("environmentEnabled", featureConfig.getEnvironmentEnabled());

        log.debug("系统配置查询: radiation={}, environment={}",
            featureConfig.getRadiationEnabled(),
            featureConfig.getEnvironmentEnabled());

        return ResponseEntity.ok(ApiResponse.success("获取系统配置成功", config));
    }
}
