package com.cdutetc.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 告警配置属性
 * 从application.yaml加载配置
 *
 * @author EMS Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ems.alert")
public class AlertProperties {

    /**
     * CPM上升率配置
     */
    private CpmRise cpmRise = new CpmRise();

    /**
     * 低电压配置
     */
    private LowBattery lowBattery = new LowBattery();

    /**
     * 离线超时配置
     */
    private OfflineTimeout offlineTimeout = new OfflineTimeout();

    /**
     * CPM上升率配置
     */
    @Data
    public static class CpmRise {
        /**
         * 上升率阈值（0.15表示15%）
         */
        private double risePercentage = 0.15;

        /**
         * 最小检查间隔（秒）
         */
        private int minInterval = 300;

        /**
         * 最小CPM值（低于此值不检查，避免基数太小导致误报）
         */
        private int minCpm = 50;
    }

    /**
     * 低电压配置
     */
    @Data
    public static class LowBattery {
        /**
         * 电压阈值（V）
         */
        private double voltageThreshold = 3.5;
    }

    /**
     * 离线超时配置
     */
    @Data
    public static class OfflineTimeout {
        /**
         * 超时时间（分钟）
         */
        private int timeoutMinutes = 10;
    }
}
