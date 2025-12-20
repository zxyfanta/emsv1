package com.ems.service.mqtt;

import com.ems.entity.device.Device;
import com.ems.repository.device.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备状态管理服务
 * 负责设备在线/离线状态的维护和更新
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusService {

    private final DeviceRepository deviceRepository;

    /**
     * 更新设备最后在线时间
     */
    public Device updateDeviceLastOnline(String deviceId, LocalDateTime timestamp) {
        try {
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseGet(() -> createNewDevice(deviceId));

            device.setLastOnlineAt(timestamp);
            device.setStatus(Device.DeviceStatus.ONLINE);

            Device savedDevice = deviceRepository.save(device);
            log.debug("更新设备最后在线时间: {}", deviceId);
            return savedDevice;

        } catch (Exception e) {
            log.error("更新设备状态失败: {}", deviceId, e);
            return null;
        }
    }

    /**
     * 将设备标记为离线
     */
    public void markDeviceOffline(String deviceId) {
        try {
            deviceRepository.findByDeviceId(deviceId)
                    .ifPresent(device -> {
                        device.setStatus(Device.DeviceStatus.OFFLINE);
                        deviceRepository.save(device);
                        log.info("设备已标记为离线: {}", deviceId);
                    });
        } catch (Exception e) {
            log.error("标记设备离线失败: {}", deviceId, e);
        }
    }

    /**
     * 批量更新设备状态
     */
    public void batchUpdateDeviceStatus() {
        try {
            // 查找需要标记为离线的设备（5分钟无数据）
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
            List<Device> devicesToOffline = deviceRepository.findOfflineDevicesBefore(threshold);

            for (Device device : devicesToOffline) {
                device.setStatus(Device.DeviceStatus.OFFLINE);
                deviceRepository.save(device);
                log.debug("设备自动离线: {}", device.getDeviceId());
            }

            if (!devicesToOffline.isEmpty()) {
                log.info("批量更新设备状态完成，{}台设备被标记为离线", devicesToOffline.size());
            }

        } catch (Exception e) {
            log.error("批量更新设备状态失败", e);
        }
    }

    /**
     * 获取在线设备数量
     */
    public long getOnlineDeviceCount() {
        try {
            return deviceRepository.findOnlineDevices().size();
        } catch (Exception e) {
            log.error("获取在线设备数量失败", e);
            return 0;
        }
    }

    /**
     * 获取离线设备数量
     */
    public long getOfflineDeviceCount() {
        try {
            return deviceRepository.count() - getOnlineDeviceCount();
        } catch (Exception e) {
            log.error("获取离线设备数量失败", e);
            return 0;
        }
    }

    /**
     * 创建新设备
     */
    private Device createNewDevice(String deviceId) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceName("设备-" + deviceId);
        device.setStatus(Device.DeviceStatus.ONLINE);
        device.setLastOnlineAt(LocalDateTime.now());

        log.info("创建新设备: {}", deviceId);
        return device;
    }

    /**
     * 手动设置设备状态
     */
    public void setDeviceStatus(String deviceId, Device.DeviceStatus status) {
        try {
            deviceRepository.findByDeviceId(deviceId)
                    .ifPresent(device -> {
                        device.setStatus(status);
                        if (status == Device.DeviceStatus.ONLINE) {
                            device.setLastOnlineAt(LocalDateTime.now());
                        }
                        deviceRepository.save(device);
                        log.info("设备状态已更新: {} -> {}", deviceId, status);
                    });
        } catch (Exception e) {
            log.error("设置设备状态失败: {} -> {}", deviceId, status, e);
        }
    }
}