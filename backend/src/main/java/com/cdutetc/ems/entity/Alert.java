package com.cdutetc.ems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警实体
 * 用于存储设备告警信息
 */
@Data
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;  // HIGH_CPM, OFFLINE, FAULT, LOW_BATTERY

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;  // CRITICAL, WARNING, INFO

    @Column(name = "device_code", length = 50)
    private String deviceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "data", columnDefinition = "JSON")
    private String data;  // JSON格式的详细数据

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (resolved == null) {
            resolved = false;
        }
    }
}
