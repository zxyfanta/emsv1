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
         * 辐射设备CPM上升率阈值
         * 默认值：0.15（15%）
         */
        private double radiationRisePercentage = 0.15;

        /**
         * 环境设备CPM上升率阈值
         * 默认值：0.15（15%）
         */
        private double environmentRisePercentage = 0.15;

        /**
         * 最小检查间隔（秒）
         */
        private int minInterval = 300;

        /**
         * 最小CPM值（低于此值不检查，避免基数太小导致误报）
         */
        private int minCpm = 50;

        /**
         * 根据设备类型获取CPM上升率阈值
         *
         * @param deviceType 设备类型（RADIATION 或 ENVIRONMENT）
         * @return 上升率阈值（0.15表示15%）
         */
        public double getRisePercentageForDevice(String deviceType) {
            if ("ENVIRONMENT".equalsIgnoreCase(deviceType)) {
                return environmentRisePercentage;
            }
            // 默认返回辐射设备阈值
            return radiationRisePercentage;
        }

        /**
         * 获取上升率阈值
         * 保留risePercentage字段以兼容旧代码
         * @deprecated 使用 getRadiationRisePercentage() 或 getRisePercentageForDevice() 代替
         */
        @Deprecated
        public double getRisePercentage() {
            return radiationRisePercentage;
        }

        /**
         * 设置上升率阈值（同时设置辐射设备阈值，保持兼容性）
         * @deprecated 使用 setRadiationRisePercentage() 代替
         */
        @Deprecated
        public void setRisePercentage(double risePercentage) {
            this.radiationRisePercentage = risePercentage;
        }
    }

    /**
     * 低电压配置
     */
    @Data
    public static class LowBattery {
        /**
         * 辐射设备低电压阈值（伏V）
         * 默认值：3.7V
         */
        private double radiationThreshold = 3.7;

        /**
         * 环境设备低电压阈值（伏V）
         * 默认值：11.1V (3.7 * 3)
         */
        private double environmentThreshold = 11.1;

        /**
         * 根据设备类型获取电压阈值
         *
         * @param deviceType 设备类型（RADIATION 或 ENVIRONMENT）
         * @return 电压阈值（伏V）
         */
        public double getThresholdForDevice(String deviceType) {
            if ("ENVIRONMENT".equalsIgnoreCase(deviceType)) {
                return environmentThreshold;
            }
            // 默认返回辐射设备阈值
            return radiationThreshold;
        }

        /**
         * 获取辐射设备电压阈值
         * 保留voltageThreshold字段以兼容旧代码
         * @deprecated 使用 getRadiationThreshold() 或 getThresholdForDevice() 代替
         */
        @Deprecated
        public double getVoltageThreshold() {
            return radiationThreshold;
        }

        /**
         * 设置电压阈值（同时设置辐射设备阈值，保持兼容性）
         * @deprecated 使用 setRadiationThreshold() 代替
         */
        @Deprecated
        public void setVoltageThreshold(double threshold) {
            this.radiationThreshold = threshold;
        }
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
