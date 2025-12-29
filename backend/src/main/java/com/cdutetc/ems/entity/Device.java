package com.cdutetc.ems.entity;

import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 设备实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ems_device")
public class Device extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "设备编码不能为空")
    @Size(max = 50, message = "设备编码长度不能超过50个字符")
    @Column(name = "device_code", nullable = false, unique = true, length = 50)
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @NotNull(message = "设备类型不能为空")
    @Column(name = "device_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    /**
     * 所属企业
     * 设备激活前为null，激活后归属到具体企业
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = true)
    private Company company;

    @NotNull(message = "设备状态不能为空")
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Size(max = 500, message = "设备描述长度不能超过500个字符")
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 255, message = "设备位置长度不能超过255个字符")
    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "last_online_at")
    private java.time.LocalDateTime lastOnlineAt;

    /**
     * 最后一次收到设备数据消息的时间
     * 用于判断设备是否在线（基于是否有数据消息）
     */
    @Column(name = "last_message_at")
    private java.time.LocalDateTime lastMessageAt;

    @Size(max = 100, message = "设备厂商长度不能超过100个字符")
    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Size(max = 50, message = "设备型号长度不能超过50个字符")
    @Column(name = "model", length = 50)
    private String model;

    @Size(max = 50, message = "设备序列号长度不能超过50个字符")
    @Column(name = "serial_number", length = 50, unique = true)
    private String serialNumber;

    @Column(name = "production_date")
    private java.time.LocalDateTime productionDate;

    /**
     * 设备激活状态
     * PENDING: 待激活（管理员录入后）
     * ACTIVE: 已激活（客户注册后）
     */
    @Column(name = "activation_status", length = 50)
    @Enumerated(EnumType.STRING)
    private DeviceActivationStatus activationStatus = DeviceActivationStatus.PENDING;

    @Column(name = "install_date")
    private java.time.LocalDateTime installDate;

    /**
     * 可视化大屏X坐标位置
     * 范围：0-100，相对于大屏中心模型的水平位置
     */
    @Column(name = "position_x")
    private Integer positionX;

    /**
     * 可视化大屏Y坐标位置
     * 范围：0-100，相对于大屏中心模型的垂直位置
     */
    @Column(name = "position_y")
    private Integer positionY;

    // ==================== 辐射设备上报专用字段 ====================
    // 注意：这些字段仅对辐射设备有效，环境设备不使用

    /**
     * 核素（如 Cs-137, Co-60）
     * 四川协议需要
     */
    @Column(name = "nuclide", length = 50)
    private String nuclide = "Cs-137";

    /**
     * 探伤机编号
     * 山东协议需要（Ma字段）
     */
    @Column(name = "inspection_machine_number", length = 50)
    private String inspectionMachineNumber;

    /**
     * 放射源编号
     * 山东协议需要（Rno字段）
     */
    @Column(name = "source_number", length = 12)
    private String sourceNumber;

    /**
     * 放射源类型
     * 01=Ⅰ类, 02=Ⅱ类, 03=Ⅲ类, 04=Ⅳ类, 05=Ⅴ类
     * 山东协议需要（Xtype字段）
     */
    @Column(name = "source_type", length = 2)
    private String sourceType = "01";

    /**
     * 放射源原始活度
     * 格式：2.700E004（科学计数法）
     * 山东协议需要（LastAct字段）
     */
    @Column(name = "original_activity", length = 20)
    private String originalActivity;

    /**
     * 放射源当前活度
     * 格式：1.300E004（科学计数法）
     * 山东协议需要（NowAct字段）
     */
    @Column(name = "current_activity", length = 20)
    private String currentActivity;

    /**
     * 放射源出厂日期
     * 山东协议需要（SourceTime字段）
     */
    @Column(name = "source_production_date")
    private java.time.LocalDate sourceProductionDate;

    // ==================== 数据上报配置字段 ====================

    /**
     * 是否启用数据上报
     */
    @Column(name = "data_report_enabled", nullable = false)
    private Boolean dataReportEnabled = false;

    /**
     * 上报协议类型
     * SICHUAN: 四川协议（HTTP + SM2加密）
     * SHANDONG: 山东协议（TCP + HJ/T212-2005）
     */
    @Column(name = "report_protocol", length = 20)
    private String reportProtocol = "SICHUAN";

    /**
     * 最后上报时间
     */
    @Column(name = "last_report_time")
    private java.time.LocalDateTime lastReportTime;

    /**
     * 最后上报状态
     * SUCCESS: 成功
     * FAILED: 失败
     */
    @Column(name = "last_report_status", length = 20)
    private String lastReportStatus;

    /**
     * 最后上报错误信息
     */
    @Column(name = "last_report_error", columnDefinition = "TEXT")
    private String lastReportError;

    /**
     * 总上报次数
     */
    @Column(name = "total_report_count")
    private Integer totalReportCount = 0;

    /**
     * 成功上报次数
     */
    @Column(name = "success_report_count")
    private Integer successReportCount = 0;
}