package com.ems.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备历史记录
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceHistoryRecord {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 记录类型
     */
    private String recordType;

    /**
     * 记录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /**
     * 记录内容
     */
    private String content;

    /**
     * 详细信息
     */
    private Map<String, Object> details;

    /**
     * 操作员
     */
    private String operator;
}