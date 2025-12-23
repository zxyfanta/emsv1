package com.cdutetc.ems.dto.response;

import com.cdutetc.ems.entity.VideoDevice;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频设备响应DTO
 */
@Data
public class VideoDeviceResponse {
    private Long id;
    private String deviceCode;
    private String deviceName;
    private String streamUrl;
    private String streamType;
    private String snapshotUrl;
    private String resolution;
    private Integer fps;
    private String status;
    private Long linkedDeviceId;
    private String linkedDeviceName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     * 注意：不返回包含认证信息的URL（username和password）
     */
    public static VideoDeviceResponse fromVideoDevice(VideoDevice videoDevice) {
        VideoDeviceResponse response = new VideoDeviceResponse();
        response.setId(videoDevice.getId());
        response.setDeviceCode(videoDevice.getDeviceCode());
        response.setDeviceName(videoDevice.getDeviceName());
        // 不返回包含认证信息的URL
        response.setStreamUrl(videoDevice.getStreamUrl());
        response.setStreamType(videoDevice.getStreamType());
        response.setSnapshotUrl(videoDevice.getSnapshotUrl());
        response.setResolution(videoDevice.getResolution());
        response.setFps(videoDevice.getFps());
        response.setStatus(videoDevice.getStatus());
        response.setCreatedAt(videoDevice.getCreatedAt());
        response.setUpdatedAt(videoDevice.getUpdatedAt());

        // 绑定的设备信息
        if (videoDevice.getLinkedDevice() != null) {
            response.setLinkedDeviceId(videoDevice.getLinkedDevice().getId());
            response.setLinkedDeviceName(videoDevice.getLinkedDevice().getDeviceName());
        }

        return response;
    }
}
