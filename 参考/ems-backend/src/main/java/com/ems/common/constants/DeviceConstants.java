package com.ems.common.constants;

/**
 * 设备相关常量
 *
 * @author EMS Team
 */
public final class DeviceConstants {

    private DeviceConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 设备状态枚举
     */
    public enum DeviceStatus {
        ONLINE("在线"),
        OFFLINE("离线"),
        UNKNOWN("未知");

        private final String description;

        DeviceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        RADIATION("辐射监测仪"),
        ENVIRONMENT("环境监测站");

        private final String description;

        DeviceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 设备缓存键前缀
     */
    public static final String DEVICE_CACHE_PREFIX = "ems:device:";

    /**
     * 设备状态缓存键前缀
     */
    public static final String DEVICE_STATUS_CACHE_PREFIX = "ems:device:status:";

    /**
     * 设备传感器数据缓存键前缀
     */
    public static final String SENSOR_DATA_CACHE_PREFIX = "ems:sensor:data:";

    /**
     * 设备分组缓存键前缀
     */
    public static final String DEVICE_GROUP_CACHE_PREFIX = "ems:device:group:";

    /**
     * 缓存过期时间（秒）
     */
    public static final long DEVICE_CACHE_EXPIRE_SECONDS = 24 * 60 * 60; // 24小时
    public static final long STATUS_CACHE_EXPIRE_SECONDS = 2 * 60 * 60; // 2小时
    public static final long SENSOR_DATA_CACHE_EXPIRE_SECONDS = 30 * 60; // 30分钟
    public static final long GROUP_CACHE_EXPIRE_SECONDS = 12 * 60 * 60; // 12小时
}