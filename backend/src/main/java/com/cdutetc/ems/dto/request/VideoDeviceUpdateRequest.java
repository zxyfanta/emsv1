package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新视频设备请求DTO
 */
@Data
public class VideoDeviceUpdateRequest {

    @Size(max = 100, message = "设备名称长度不能超过100")
    private String deviceName;

    @Size(max = 500, message = "URL长度不能超过500")
    private String streamUrl;

    private String streamType;

    private String snapshotUrl;
    private String username;
    private String password;
    private String resolution;
    private Integer fps;
    private String status;
}
