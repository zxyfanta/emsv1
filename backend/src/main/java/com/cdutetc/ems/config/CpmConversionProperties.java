package com.cdutetc.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CPM转换系数配置
 * 用于将不同设备类型发送的原始CPM值转换为标准CPM单位
 *
 * @author EMS Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ems.mqtt.cpm")
public class CpmConversionProperties {

    /**
     * 辐射设备CPM转换系数
     * 默认值：10 (表示原始CPM值需要除以10得到标准CPM)
     * 用法：convertedCpm = rawCpm / radiationConversionFactor
     */
    private double radiationConversionFactor = 10.0;

    /**
     * 环境设备CPM转换系数
     * 默认值：634 (表示原始CPM值需要除以634得到标准CPM)
     * 用法：convertedCpm = rawCpm / environmentConversionFactor
     */
    private double environmentConversionFactor = 634.0;

    /**
     * 是否启用CPM转换
     * 默认值：true (启用转换)
     * 如果设置为false，则直接使用原始CPM值，不进行转换
     */
    private boolean enabled = true;
}
