package com.ems.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备规格信息
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSpecifications {

    /**
     * 设备型号
     */
    private String model;

    /**
     * 制造商
     */
    private String manufacturer;

    /**
     * 版本号
     */
    private String version;

    /**
     * 序列号
     */
    private String serialNumber;

    /**
     * 安装日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime installDate;

    /**
     * 保修期
     */
    private String warrantyPeriod;

    /**
     * 技术参数
     */
    private Map<String, Object> technicalParams;
}