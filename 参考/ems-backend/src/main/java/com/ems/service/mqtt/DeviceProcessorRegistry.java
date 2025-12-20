package com.ems.service.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 设备处理器注册组件
 * 在系统启动时自动注册所有设备数据处理器
 *
 * @author EMS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceProcessorRegistry {

    private final DeviceTypeRouter deviceTypeRouter;
    private final RadiationDataProcessor radiationDataProcessor;
    private final EnvironmentDataProcessor environmentDataProcessor;

    /**
     * 系统启动时注册所有处理器
     */
    @PostConstruct
    public void registerProcessors() {
        try {
            // 注册辐射设备处理器
            deviceTypeRouter.registerProcessor("RADIATION", radiationDataProcessor);
            log.info("✅ 已注册辐射设备处理器");

            // 注册环境监测设备处理器
            deviceTypeRouter.registerProcessor("ENVIRONMENT", environmentDataProcessor);
            log.info("✅ 已注册环境监测设备处理器");

            log.info("🔧 设备类型处理器注册完成，共注册{}个处理器", 2);

        } catch (Exception e) {
            log.error("❌ 设备类型处理器注册失败", e);
            throw new RuntimeException("设备类型处理器注册失败", e);
        }
    }

    /**
     * 获取处理器注册状态
     */
    public String getRegistryStatus() {
        var stats = deviceTypeRouter.getStatistics();
        return String.format("设备处理器注册状态: 已注册处理器数量=%d, 热点缓存大小=%d",
                           stats.getRegisteredProcessors(), stats.getHotCacheSize());
    }
}