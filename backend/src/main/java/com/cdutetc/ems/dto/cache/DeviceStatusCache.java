package com.cdutetc.ems.dto.cache;

import lombok.Data;

/**
 * 设备状态缓存DTO
 * 用于Redis中存储设备实时状态
 *
 * @author EMS Team
 */
@Data
public class DeviceStatusCache {

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 设备ID（数据库主键）
     */
    private Long deviceId;

    /**
     * 所属企业ID
     */
    private Long companyId;

    /**
     * 最后消息时间（ISO格式字符串）
     */
    private String lastMessageAt;

    /**
     * 最后CPM值（辐射值）
     */
    private Double lastCpm;

    /**
     * 最后电压值（电池电压）
     */
    private Double lastBattery;

    /**
     * 设备状态（ONLINE, OFFLINE等）
     */
    private String status;

    /**
     * 获取最后消息时间用于缓存设置
     * 这是一个辅助方法，用于在设置缓存时
     */
    public String getLastMessageAt() {
        return lastMessageAt;
    }

    /**
     * 检查设备是否在线
     * 基于最后消息时间判断
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return true表示在线，false表示离线
     */
    public boolean isOnline(int timeoutMinutes) {
        if (lastMessageAt == null) {
            return false;
        }

        try {
            java.time.LocalDateTime lastMessageTime = java.time.LocalDateTime.parse(
                lastMessageAt,
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(lastMessageTime, now);

            return duration.toMinutes() <= timeoutMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算CPM上升率
     *
     * @param previousCpm 上一次的CPM值
     * @return 上升率（0.15表示15%），如果无法计算则返回null
     */
    public Double calculateCpmRiseRate(Double previousCpm) {
        if (previousCpm == null || lastCpm == null || previousCpm == 0) {
            return null;
        }

        return (lastCpm - previousCpm) / previousCpm;
    }

    /**
     * 检查是否低电压
     *
     * @param threshold 电压阈值（V）
     * @return true表示低电压
     */
    public boolean isLowBattery(double threshold) {
        return lastBattery != null && lastBattery < threshold;
    }
}
