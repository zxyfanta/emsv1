package com.ems.service;

import com.ems.entity.device.Device;
import com.ems.entity.enterprise.Enterprise;
import com.ems.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeviceService 单元测试
 *
 * @author EMS Team
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    private Enterprise testEnterprise;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        // 创建测试企业
        testEnterprise = Enterprise.builder()
                .id(1L)
                .name("测试企业")
                .build();

        // 创建测试设备
        testDevice = Device.builder()
                .id(1L)
                .deviceId("TEST-001")
                .deviceName("测试设备")
                .deviceType(Device.DeviceType.RADIATION)
                .status(Device.DeviceStatus.ONLINE)
                .enterprise(testEnterprise)
                .build();
    }

    @Test
    void testGetDeviceById_Success() {
        // Given
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

        // When
        Optional<Device> result = deviceService.getDeviceById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testDevice.getDeviceId(), result.get().getDeviceId());
        verify(deviceRepository, times(1)).findById(1L);
    }

    @Test
    void testGetDeviceById_NotFound() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Device> result = deviceService.getDeviceById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(deviceRepository, times(1)).findById(999L);
    }

    @Test
    void testGetDeviceByDeviceId_Success() {
        // Given
        when(deviceRepository.findByDeviceId("TEST-001")).thenReturn(Optional.of(testDevice));

        // When
        Optional<Device> result = deviceService.getDeviceByDeviceId("TEST-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testDevice.getId(), result.get().getId());
        verify(deviceRepository, times(1)).findByDeviceId("TEST-001");
    }

    @Test
    void testGetDevicesByEnterprise() {
        // Given
        List<Device> devices = Arrays.asList(testDevice);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Device> devicePage = new PageImpl<>(devices, pageable, devices.size());

        when(deviceRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                .thenReturn(devicePage);

        // When
        Page<Device> result = deviceService.getDevicesByEnterprise(1L, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(testDevice.getDeviceId(), result.getContent().get(0).getDeviceId());
        verify(deviceRepository, times(1)).findByEnterpriseIdAndDeletedFalse(eq(1L), any(Pageable.class));
    }

    @Test
    void testCreateDevice() {
        // Given
        Device newDevice = Device.builder()
                .deviceId("TEST-002")
                .deviceName("新设备")
                .deviceType(Device.DeviceType.ENVIRONMENT)
                .enterprise(testEnterprise)
                .build();

        Device savedDevice = Device.builder()
                .id(2L)
                .deviceId("TEST-002")
                .deviceName("新设备")
                .deviceType(Device.DeviceType.ENVIRONMENT)
                .status(Device.DeviceStatus.OFFLINE)
                .enterprise(testEnterprise)
                .build();

        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        // When
        Device result = deviceService.createDevice(newDevice);

        // Then
        assertNotNull(result.getId());
        assertEquals("TEST-002", result.getDeviceId());
        assertEquals(Device.DeviceStatus.OFFLINE, result.getStatus());
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void testUpdateDeviceStatus() {
        // Given
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        Device result = deviceService.updateDeviceStatus(1L, Device.DeviceStatus.OFFLINE);

        // Then
        assertEquals(Device.DeviceStatus.OFFLINE, result.getStatus());
        verify(deviceRepository, times(1)).findById(1L);
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void testUpdateDeviceStatus_NotFound() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deviceService.updateDeviceStatus(999L, Device.DeviceStatus.OFFLINE);
        });

        verify(deviceRepository, times(1)).findById(999L);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void testDeleteDevice() {
        // Given
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        boolean result = deviceService.deleteDevice(1L);

        // Then
        assertTrue(result);
        verify(deviceRepository, times(1)).findById(1L);
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void testDeleteDevice_NotFound() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = deviceService.deleteDevice(999L);

        // Then
        assertFalse(result);
        verify(deviceRepository, times(1)).findById(999L);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void testSearchDevices() {
        // Given
        List<Device> devices = Arrays.asList(testDevice);
        when(deviceRepository.findByDeviceNameContainingIgnoreCaseAndDeletedFalse("测试"))
                .thenReturn(devices);

        // When
        List<Device> result = deviceService.searchDevices("测试");

        // Then
        assertEquals(1, result.size());
        assertEquals(testDevice.getDeviceId(), result.get(0).getDeviceId());
        verify(deviceRepository, times(1)).findByDeviceNameContainingIgnoreCaseAndDeletedFalse("测试");
    }
}