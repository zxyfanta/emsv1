package com.cdutetc.ems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频设备实体
 * 用于管理第三方视频流设备，支持绑定到监测设备
 */
@Data
@Entity
@Table(name = "video_devices")
public class VideoDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", unique = true, nullable = false, length = 50)
    private String deviceCode;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "stream_url", length = 500)
    private String streamUrl;

    @Column(name = "stream_type", length = 20)
    private String streamType;

    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "fps")
    private Integer fps;

    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_device_id")
    private Device linkedDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "OFFLINE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
