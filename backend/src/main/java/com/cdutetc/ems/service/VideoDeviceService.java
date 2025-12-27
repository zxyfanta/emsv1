package com.cdutetc.ems.service;

import com.cdutetc.ems.dto.request.VideoDeviceCreateRequest;
import com.cdutetc.ems.dto.request.VideoDeviceUpdateRequest;
import com.cdutetc.ems.dto.response.VideoDeviceResponse;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.VideoDevice;
import com.cdutetc.ems.exception.ResourceNotFoundException;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.repository.VideoDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 视频设备服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoDeviceService {

    private final VideoDeviceRepository videoDeviceRepository;
    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;

    /**
     * 创建视频设备
     */
    @Transactional
    public VideoDevice createVideoDevice(VideoDeviceCreateRequest request, Long companyId) {
        // 检查编码唯一性
        if (videoDeviceRepository.existsByDeviceCode(request.getDeviceCode())) {
            throw new IllegalArgumentException("视频设备编码已存在: " + request.getDeviceCode());
        }

        VideoDevice videoDevice = new VideoDevice();
        videoDevice.setDeviceCode(request.getDeviceCode());
        videoDevice.setDeviceName(request.getDeviceName());
        videoDevice.setStreamUrl(request.getStreamUrl());
        videoDevice.setStreamType(request.getStreamType() != null ? request.getStreamType() : "RTSP");
        videoDevice.setSnapshotUrl(request.getSnapshotUrl());
        videoDevice.setUsername(request.getUsername());
        videoDevice.setPassword(request.getPassword());
        videoDevice.setResolution(request.getResolution());
        videoDevice.setFps(request.getFps());
        videoDevice.setStatus("OFFLINE");

        // 设置企业
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("企业不存在"));
        videoDevice.setCompany(company);

        VideoDevice saved = videoDeviceRepository.save(videoDevice);
        log.info("创建视频设备成功: deviceCode={}, companyId={}", request.getDeviceCode(), companyId);
        return saved;
    }

    /**
     * 更新视频设备
     */
    @Transactional
    public VideoDevice updateVideoDevice(Long id, VideoDeviceUpdateRequest request, Long companyId) {
        VideoDevice videoDevice = videoDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频设备不存在"));

        // 验证权限
        if (!videoDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限操作此视频设备");
        }

        // 更新字段
        if (request.getDeviceName() != null) {
            videoDevice.setDeviceName(request.getDeviceName());
        }
        if (request.getStreamUrl() != null) {
            videoDevice.setStreamUrl(request.getStreamUrl());
        }
        if (request.getStreamType() != null) {
            videoDevice.setStreamType(request.getStreamType());
        }
        if (request.getSnapshotUrl() != null) {
            videoDevice.setSnapshotUrl(request.getSnapshotUrl());
        }
        if (request.getUsername() != null) {
            videoDevice.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            videoDevice.setPassword(request.getPassword());
        }
        if (request.getResolution() != null) {
            videoDevice.setResolution(request.getResolution());
        }
        if (request.getFps() != null) {
            videoDevice.setFps(request.getFps());
        }
        if (request.getStatus() != null) {
            videoDevice.setStatus(request.getStatus());
        }

        VideoDevice updated = videoDeviceRepository.save(videoDevice);
        log.info("更新视频设备成功: id={}, deviceCode={}", id, videoDevice.getDeviceCode());
        return updated;
    }

    /**
     * 绑定视频设备到监测设备
     * 一个监测设备只能绑定一个视频设备（一对一关系）
     */
    @Transactional
    public VideoDevice bindToDevice(Long videoDeviceId, Long monitorDeviceId, Long companyId) {
        VideoDevice videoDevice = videoDeviceRepository.findById(videoDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("视频设备不存在"));

        // 验证权限
        if (!videoDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限操作此视频设备");
        }

        // 查找监测设备
        Device monitorDevice = deviceRepository.findById(monitorDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("监测设备不存在"));

        // 验证监测设备属于同一企业
        if (!monitorDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("监测设备不属于同一企业");
        }

        // 检查该监测设备是否已被其他视频设备绑定
        Optional<VideoDevice> existingBinding = videoDeviceRepository.findByLinkedDeviceId(monitorDeviceId);
        if (existingBinding.isPresent() && !existingBinding.get().getId().equals(videoDeviceId)) {
            VideoDevice existing = existingBinding.get();
            throw new IllegalStateException(
                String.format("该监测设备已绑定视频设备[%s]，请先解绑。当前操作: 绑定[%s]到[%s]",
                    existing.getDeviceCode(),
                    videoDevice.getDeviceCode(),
                    monitorDevice.getDeviceCode())
            );
        }

        videoDevice.setLinkedDevice(monitorDevice);
        VideoDevice updated = videoDeviceRepository.save(videoDevice);
        log.info("绑定视频设备成功: videoDevice={}, monitorDevice={}",
                videoDevice.getDeviceCode(), monitorDevice.getDeviceCode());
        return updated;
    }

    /**
     * 解绑视频设备
     */
    @Transactional
    public VideoDevice unbind(Long videoDeviceId, Long companyId) {
        VideoDevice videoDevice = videoDeviceRepository.findById(videoDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("视频设备不存在"));

        if (!videoDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限操作此视频设备");
        }

        videoDevice.setLinkedDevice(null);
        VideoDevice updated = videoDeviceRepository.save(videoDevice);
        log.info("解绑视频设备成功: deviceCode={}", videoDevice.getDeviceCode());
        return updated;
    }

    /**
     * 获取视频设备的流URL（返回带认证信息的完整URL）
     * 用于前端播放器直接播放
     */
    public String getStreamUrl(Long videoDeviceId, Long companyId) {
        VideoDevice videoDevice = videoDeviceRepository.findById(videoDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("视频设备不存在"));

        if (!videoDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限访问此视频设备");
        }

        String url = videoDevice.getStreamUrl();

        // 如果有认证信息，添加到URL中
        if (videoDevice.getUsername() != null && videoDevice.getPassword() != null) {
            // RTSP: rtsp://user:pass@ip:port/path
            // HTTP: http://user:pass@ip:port/path
            String protocol = url.substring(0, url.indexOf("://") + 3);
            String rest = url.substring(url.indexOf("://") + 3);
            url = protocol + videoDevice.getUsername() + ":" + videoDevice.getPassword() + "@" + rest;
        }

        return url;
    }

    /**
     * 获取企业下的所有视频设备
     */
    public Page<VideoDevice> getVideoDevices(Long companyId, Pageable pageable) {
        return videoDeviceRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * 获取企业下的所有视频设备（不分页）
     */
    public List<VideoDevice> getAllVideoDevices(Long companyId) {
        return videoDeviceRepository.findAllByCompanyId(companyId);
    }

    /**
     * 获取未绑定的视频设备
     */
    public List<VideoDevice> getUnboundVideoDevices(Long companyId) {
        return videoDeviceRepository.findUnboundByCompanyId(companyId);
    }

    /**
     * 获取监测设备绑定的视频设备
     */
    public VideoDevice getVideoDeviceByMonitorDevice(Long monitorDeviceId) {
        return videoDeviceRepository.findByLinkedDeviceId(monitorDeviceId).orElse(null);
    }

    /**
     * 根据ID获取视频设备
     */
    public VideoDevice getVideoDeviceById(Long id, Long companyId) {
        VideoDevice videoDevice = videoDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频设备不存在"));

        if (!videoDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限访问此视频设备");
        }

        return videoDevice;
    }

    /**
     * 删除视频设备
     */
    @Transactional
    public void deleteVideoDevice(Long id, Long companyId) {
        VideoDevice videoDevice = videoDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频设备不存在"));

        if (!videoDevice.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("无权限删除此视频设备");
        }

        videoDeviceRepository.delete(videoDevice);
        log.info("删除视频设备成功: id={}, deviceCode={}", id, videoDevice.getDeviceCode());
    }
}
