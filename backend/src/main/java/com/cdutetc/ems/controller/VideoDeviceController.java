package com.cdutetc.ems.controller;

import com.cdutetc.ems.dto.request.VideoDeviceCreateRequest;
import com.cdutetc.ems.dto.request.VideoDeviceUpdateRequest;
import com.cdutetc.ems.dto.response.VideoDeviceResponse;
import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.VideoDevice;
import com.cdutetc.ems.service.VideoDeviceService;
import com.cdutetc.ems.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 视频设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/video-devices")
@RequiredArgsConstructor
public class VideoDeviceController {

    private final VideoDeviceService videoDeviceService;

    /**
     * 创建视频设备
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VideoDeviceResponse>> createVideoDevice(
            @Valid @RequestBody VideoDeviceCreateRequest request) {

        User currentUser = getCurrentUser();
        VideoDevice videoDevice = videoDeviceService.createVideoDevice(
                request,
                currentUser.getCompany().getId()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "创建视频设备成功",
                VideoDeviceResponse.fromVideoDevice(videoDevice)
        ));
    }

    /**
     * 获取企业下的所有视频设备（分页）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VideoDeviceResponse>>> getVideoDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = getCurrentUser();
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<VideoDevice> devices = videoDeviceService.getVideoDevices(
                currentUser.getCompany().getId(),
                pageable
        );

        Page<VideoDeviceResponse> responses = devices.map(VideoDeviceResponse::fromVideoDevice);
        return ResponseEntity.ok(ApiResponse.success("获取视频设备列表成功", responses));
    }

    /**
     * 获取企业下的所有视频设备（不分页）
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<VideoDeviceResponse>>> getAllVideoDevices() {
        User currentUser = getCurrentUser();
        List<VideoDevice> devices = videoDeviceService.getAllVideoDevices(
                currentUser.getCompany().getId()
        );

        List<VideoDeviceResponse> responses = devices.stream()
                .map(VideoDeviceResponse::fromVideoDevice)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取视频设备列表成功", responses));
    }

    /**
     * 获取视频设备详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDeviceResponse>> getVideoDevice(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        VideoDevice videoDevice = videoDeviceService.getVideoDeviceById(id, currentUser.getCompany().getId());

        return ResponseEntity.ok(ApiResponse.success(
                "获取视频设备成功",
                VideoDeviceResponse.fromVideoDevice(videoDevice)
        ));
    }

    /**
     * 更新视频设备
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDeviceResponse>> updateVideoDevice(
            @PathVariable Long id,
            @Valid @RequestBody VideoDeviceUpdateRequest request) {

        User currentUser = getCurrentUser();
        VideoDevice videoDevice = videoDeviceService.updateVideoDevice(
                id,
                request,
                currentUser.getCompany().getId()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "更新视频设备成功",
                VideoDeviceResponse.fromVideoDevice(videoDevice)
        ));
    }

    /**
     * 获取视频流URL（用于前端播放器）
     * 返回带认证信息的完整URL
     */
    @GetMapping("/{id}/stream-url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStreamUrl(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        VideoDevice videoDevice = videoDeviceService.getVideoDeviceById(id, currentUser.getCompany().getId());
        String streamUrl = videoDeviceService.getStreamUrl(id, currentUser.getCompany().getId());

        return ResponseEntity.ok(ApiResponse.success("获取视频流URL成功", Map.of(
                "deviceId", id,
                "deviceCode", videoDevice.getDeviceCode(),
                "streamType", videoDevice.getStreamType(),
                "streamUrl", streamUrl,
                "snapshotUrl", videoDevice.getSnapshotUrl()
        )));
    }

    /**
     * 绑定视频设备到监测设备
     */
    @PostMapping("/{videoDeviceId}/bind")
    public ResponseEntity<ApiResponse<VideoDeviceResponse>> bindToDevice(
            @PathVariable Long videoDeviceId,
            @RequestParam Long monitorDeviceId) {

        User currentUser = getCurrentUser();
        VideoDevice videoDevice = videoDeviceService.bindToDevice(
                videoDeviceId,
                monitorDeviceId,
                currentUser.getCompany().getId()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "绑定成功",
                VideoDeviceResponse.fromVideoDevice(videoDevice)
        ));
    }

    /**
     * 解绑视频设备
     */
    @PostMapping("/{videoDeviceId}/unbind")
    public ResponseEntity<ApiResponse<VideoDeviceResponse>> unbind(
            @PathVariable Long videoDeviceId) {

        User currentUser = getCurrentUser();
        VideoDevice videoDevice = videoDeviceService.unbind(videoDeviceId, currentUser.getCompany().getId());

        return ResponseEntity.ok(ApiResponse.success(
                "解绑成功",
                VideoDeviceResponse.fromVideoDevice(videoDevice)
        ));
    }

    /**
     * 获取未绑定的视频设备
     */
    @GetMapping("/unbound")
    public ResponseEntity<ApiResponse<List<VideoDeviceResponse>>> getUnboundDevices() {
        User currentUser = getCurrentUser();
        List<VideoDevice> devices = videoDeviceService.getUnboundVideoDevices(
                currentUser.getCompany().getId()
        );

        List<VideoDeviceResponse> responses = devices.stream()
                .map(VideoDeviceResponse::fromVideoDevice)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取未绑定视频设备成功", responses));
    }

    /**
     * 获取监测设备绑定的视频设备
     */
    @GetMapping("/by-monitor/{monitorDeviceId}")
    public ResponseEntity<ApiResponse<VideoDeviceResponse>> getByMonitorDevice(
            @PathVariable Long monitorDeviceId) {

        VideoDevice videoDevice = videoDeviceService.getVideoDeviceByMonitorDevice(monitorDeviceId);

        if (videoDevice == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound("该监测设备未绑定视频设备"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                VideoDeviceResponse.fromVideoDevice(videoDevice)
        ));
    }

    /**
     * 删除视频设备
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideoDevice(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        videoDeviceService.deleteVideoDevice(id, currentUser.getCompany().getId());
        return ResponseEntity.ok(ApiResponse.success("删除视频设备成功", null));
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}
