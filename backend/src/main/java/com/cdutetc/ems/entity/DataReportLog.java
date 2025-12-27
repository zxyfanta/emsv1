package com.cdutetc.ems.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 数据上报日志实体
 * 记录设备上报的详细信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ems_data_report_log",
       indexes = {
           @Index(name = "idx_device_time", columnList = "device_id,report_time"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_protocol", columnList = "report_protocol")
       })
public class DataReportLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设备ID
     */
    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    /**
     * 设备编码（冗余字段，方便查询）
     */
    @Column(name = "device_code", length = 50)
    private String deviceCode;

    /**
     * 上报协议
     * SICHUAN: 四川协议（HTTP + SM2加密）
     * SHANDONG: 山东协议（TCP + HJ/T212-2005）
     */
    @Column(name = "report_protocol", nullable = false, length = 20)
    private String reportProtocol;

    /**
     * 上报时间
     */
    @Column(name = "report_time", nullable = false)
    private java.time.LocalDateTime reportTime;

    /**
     * 请求内容（JSON或HJ/T212报文）
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * 响应内容
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /**
     * 状态
     * SUCCESS: 成功
     * FAILED: 失败
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * HTTP状态码（仅四川协议）
     */
    @Column(name = "http_status")
    private Integer httpStatus;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 关联的设备实体（查询时使用）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", insertable = false, updatable = false)
    private Device device;
}
