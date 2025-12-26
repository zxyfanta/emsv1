package com.cdutetc.ems.service;

import com.cdutetc.ems.dto.request.DeviceImportItem;
import com.cdutetc.ems.dto.response.*;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.DeviceActivationCode;
import com.cdutetc.ems.entity.enums.ActivationCodeStatus;
import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.DeviceActivationCodeRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 设备激活服务类
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeviceActivationService {

    private final DeviceRepository deviceRepository;
    private final DeviceActivationCodeRepository activationCodeRepository;

    /**
     * 批量导入设备并生成激活码
     * 设备初始状态不绑定企业，由用户激活时绑定
     */
    public BatchImportResult batchImportDevices(List<DeviceImportItem> items) {
        List<Device> importedDevices = new ArrayList<>();
        List<DeviceActivationCode> generatedCodes = new ArrayList<>();

        for (DeviceImportItem item : items) {
            try {
                // 1. 检查设备编码是否已存在
                if (deviceRepository.existsByDeviceCode(item.getDeviceCode())) {
                    log.warn("⚠️ 设备编码 {} 已存在，跳过", item.getDeviceCode());
                    continue;
                }

                // 2. 检查序列号是否已存在
                if (deviceRepository.existsBySerialNumber(item.getSerialNumber())) {
                    log.warn("⚠️ 序列号 {} 已存在，跳过", item.getSerialNumber());
                    continue;
                }

                // 3. 创建设备
                Device device = new Device();
                device.setDeviceCode(item.getDeviceCode());
                device.setDeviceName(item.getDeviceCode());  // 默认使用设备编码作为名称
                device.setDeviceType(DeviceType.valueOf(item.getDeviceType()));
                device.setSerialNumber(item.getSerialNumber());
                device.setManufacturer(item.getManufacturer());
                device.setModel(item.getModel());
                device.setProductionDate(item.getProductionDate());
                device.setActivationStatus(DeviceActivationStatus.PENDING);
                device.setStatus(DeviceStatus.OFFLINE);
                device.setCreatedAt(LocalDateTime.now());
                device.setUpdatedAt(LocalDateTime.now());
                // 注意：设备初始不绑定企业，由用户激活时绑定

                device = deviceRepository.save(device);
                importedDevices.add(device);

                // 4. 生成激活码
                DeviceActivationCode activationCode = generateActivationCode(device);
                generatedCodes.add(activationCode);

                log.info("✅ 设备录入成功: {} ({})", device.getDeviceCode(), device.getSerialNumber());

            } catch (Exception e) {
                log.error("❌ 导入设备失败: {}, 错误: {}", item.getDeviceCode(), e.getMessage());
            }
        }

        // 5. 构建响应
        return BatchImportResult.builder()
                .totalCount(items.size())
                .importedCount(importedDevices.size())
                .devices(importedDevices.stream()
                        .map(DeviceResponse::fromDevice)
                        .toList())
                .activationCodes(generatedCodes.stream()
                        .map(ActivationCodeResponse::fromEntity)
                        .toList())
                .build();
    }

    /**
     * 生成激活码
     */
    private DeviceActivationCode generateActivationCode(Device device) {
        String code;
        do {
            code = generateUniqueCode(device.getDeviceType());
        } while (activationCodeRepository.existsByCode(code));

        DeviceActivationCode activationCode = DeviceActivationCode.builder()
                .code(code)
                .device(device)
                .status(ActivationCodeStatus.UNUSED)
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))  // 1年有效期
                .build();

        return activationCodeRepository.save(activationCode);
    }

    /**
     * 生成唯一激活码
     * 格式: EMS-{设备类型缩写}-{随机码8位}
     * 例如: EMS-RAD-X7K9P3M2
     */
    private String generateUniqueCode(DeviceType deviceType) {
        String prefix = deviceType == DeviceType.RADIATION_MONITOR ? "RAD" : "ENV";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String randomCode = sb.toString();
        return String.format("EMS-%s-%s", prefix, randomCode);
    }

    /**
     * 查询待激活设备列表
     */
    @Transactional(readOnly = true)
    public List<PendingDeviceResponse> getPendingDevices() {
        List<Device> pendingDevices = deviceRepository.findByActivationStatus(DeviceActivationStatus.PENDING);

        return pendingDevices.stream()
                .map(device -> {
                    DeviceActivationCode code = activationCodeRepository.findByDevice(device).orElse(null);
                    if (code == null) {
                        log.warn("⚠️ 设备 {} 没有激活码", device.getDeviceCode());
                        return null;
                    }
                    return PendingDeviceResponse.fromEntity(device, code);
                })
                .filter(response -> response != null)
                .toList();
    }

    /**
     * 查询已激活设备列表
     */
    @Transactional(readOnly = true)
    public List<ActivatedDeviceResponse> getActivatedDevices() {
        List<Device> activatedDevices = deviceRepository.findByActivationStatus(DeviceActivationStatus.ACTIVE);

        return activatedDevices.stream()
                .map(device -> {
                    DeviceActivationCode code = activationCodeRepository.findByDevice(device).orElse(null);
                    if (code == null) {
                        log.warn("⚠️ 设备 {} 没有激活码", device.getDeviceCode());
                        return null;
                    }
                    return ActivatedDeviceResponse.fromEntity(device, code);
                })
                .filter(response -> response != null)
                .toList();
    }

    /**
     * 验证激活码
     */
    @Transactional(readOnly = true)
    public ActivationCodeInfo verifyActivationCode(String activationCode) {
        DeviceActivationCode code = activationCodeRepository.findByCode(activationCode)
                .orElseThrow(() -> new IllegalArgumentException("激活码不存在"));

        // 检查状态
        if (code.getStatus() == ActivationCodeStatus.USED) {
            throw new IllegalArgumentException("激活码已被使用");
        }

        if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("激活码已过期");
        }

        // 返回设备信息
        Device device = code.getDevice();
        return ActivationCodeInfo.builder()
                .code(code.getCode())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType().name())
                .serialNumber(device.getSerialNumber())
                .manufacturer(device.getManufacturer())
                .model(device.getModel())
                .productionDate(device.getProductionDate())
                .expiresAt(code.getExpiresAt())
                .build();
    }

    /**
     * 激活设备
     */
    public Device activateDevice(String activationCodeStr, String deviceName, String description,
                                String location, Integer positionX, Integer positionY,
                                Long companyId, Long userId, String username, String clientIp) {

        // 1. 验证激活码
        DeviceActivationCode activationCode = activationCodeRepository.findByCode(activationCodeStr)
                .orElseThrow(() -> new IllegalArgumentException("激活码不存在"));

        // 2. 检查激活码状态
        if (activationCode.getStatus() == ActivationCodeStatus.USED) {
            throw new IllegalArgumentException("激活码已被使用");
        }

        if (activationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("激活码已过期");
        }

        // 3. 获取设备
        Device device = activationCode.getDevice();

        // 4. 更新设备信息
        device.setDeviceName(deviceName);
        device.setDescription(description);
        device.setLocation(location);
        device.setPositionX(positionX);
        device.setPositionY(positionY);

        // 5. 归属到企业
        Company company = new Company();
        company.setId(companyId);
        device.setCompany(company);

        // 6. 更新激活状态
        device.setActivationStatus(DeviceActivationStatus.ACTIVE);

        device = deviceRepository.save(device);

        // 7. 更新激活码状态
        activationCode.setStatus(ActivationCodeStatus.USED);
        activationCode.setUsedAt(LocalDateTime.now());
        activationCode.setUsedByCompany(company);
        activationCode.setUsedByUserId(userId);
        activationCode.setUsedByUsername(username);
        activationCode.setUsedIpAddress(clientIp);
        activationCodeRepository.save(activationCode);

        log.info("✅ 设备 {} 激活成功，企业ID: {}, 用户: {}", device.getDeviceCode(), companyId, username);

        return device;
    }

    /**
     * 获取设备的激活码信息
     */
    @Transactional(readOnly = true)
    public ActivationCodeResponse getDeviceActivationCode(Device device) {
        return activationCodeRepository.findByDevice(device)
                .map(ActivationCodeResponse::fromEntity)
                .orElse(null);
    }
}
