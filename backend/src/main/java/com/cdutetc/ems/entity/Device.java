package com.cdutetc.ems.entity;

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
import lombok.Builder;

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

    @NotNull(message = "所属企业不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
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

    @Size(max = 100, message = "设备厂商长度不能超过100个字符")
    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Size(max = 50, message = "设备型号长度不能超过50个字符")
    @Column(name = "model", length = 50)
    private String model;

    @Size(max = 50, message = "设备序列号长度不能超过50个字符")
    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Column(name = "install_date")
    private java.time.LocalDateTime installDate;
}