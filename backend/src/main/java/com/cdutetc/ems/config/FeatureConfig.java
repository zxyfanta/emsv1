package com.cdutetc.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统功能开关配置
 * 从application.yaml中读取 app.ems.features 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ems.features")
public class FeatureConfig {

    /**
     * 是否启用辐射监测功能
     * 启用后将显示：设备管理、辐射数据模块
     */
    private Boolean radiationEnabled = true;

    /**
     * 是否启用环境监测功能
     * 启用后将显示：设备管理、环境数据模块
     */
    private Boolean environmentEnabled = true;
}
