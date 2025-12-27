package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建视频设备请求DTO
 */
@Data
public class VideoDeviceCreateRequest {

    @NotBlank(message = "视频设备编码不能为空")
    @Size(max = 50, message = "设备编码长度不能超过50")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100")
    private String deviceName;

    @NotBlank(message = "视频流URL不能为空")
    @Size(max = 500, message = "URL长度不能超过500")
    private String streamUrl;

    private String streamType;  // RTSP, RTMP, HLS, FLV, WEBRTC

    private String snapshotUrl;
    private String username;
    private String password;
    private String resolution;
    private Integer fps;
}
