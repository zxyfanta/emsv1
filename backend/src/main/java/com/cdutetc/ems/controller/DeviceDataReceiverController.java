package com.cdutetc.ems.controller;

import com.cdutetc.ems.config.CpmConversionProperties;
import com.cdutetc.ems.util.ApiResponse;
import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.cdutetc.ems.dto.request.RadiationDataReceiveRequest;
import com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest;
import com.cdutetc.ems.dto.response.DeviceDataReceiveResponse;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.service.DeviceService;
import com.cdutetc.ems.service.RadiationDeviceDataService;
import com.cdutetc.ems.service.EnvironmentDeviceDataService;
import com.cdutetc.ems.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备数据接收控制器
 * 处理设备上报的监测数据
 */
@Slf4j
@RestController
@RequestMapping("/device-data")
@RequiredArgsConstructor
public class DeviceDataReceiverController {

    private final DeviceService deviceService;
    private final RadiationDeviceDataService radiationDeviceDataService;
    private final EnvironmentDeviceDataService environmentDeviceDataService;
    private final SseEmitterService sseEmitterService;
    private final CpmConversionProperties cpmConversionProperties;

    /**
     * 接收辐射设备数据
     */
    @PostMapping("/radiation")
    public ResponseEntity<ApiResponse<DeviceDataReceiveResponse>> receiveRadiationData(
            @Valid @RequestBody RadiationDataReceiveRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        log.info("接收到辐射设备数据上报 - 设备编码: {}, IP: {}", request.getDeviceCode(), clientIp);

        try {
            // 验证设备是否存在
            Device device = deviceService.findByDeviceCode(request.getDeviceCode());
            if (device == null) {
                log.warn("设备不存在: {}", request.getDeviceCode());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("设备不存在: " + request.getDeviceCode()));
            }

            // 更新设备最后在线时间
            device.setLastOnlineAt(LocalDateTime.now());
            deviceService.updateDevice(device.getId(), device, device.getCompany().getId());

            // 创建辐射设备数据记录
            RadiationDeviceData data = new RadiationDeviceData();
            data.setDeviceCode(request.getDeviceCode());
            data.setRawData(request.getRawData());
            data.setSrc(request.getSrc());
            data.setMsgtype(request.getMsgtype());

            // 应用CPM转换系数
            Double rawCpm = request.getCpm();
            if (rawCpm != null) {
                double convertedCpm = cpmConversionProperties.isEnabled()
                    ? rawCpm / cpmConversionProperties.getRadiationConversionFactor()
                    : rawCpm;
                data.setCpm(convertedCpm);
                if (cpmConversionProperties.isEnabled()) {
                    log.debug("🔄 REST API辐射设备CPM转换: 设备={}, 原始值={}, 转换系数={}, 转换后值={}",
                        request.getDeviceCode(), rawCpm,
                        cpmConversionProperties.getRadiationConversionFactor(), convertedCpm);
                }
            }

            // 辐射设备发送的是毫伏mV，需要转换为伏V存储
            Double rawBatvolt = request.getBatvolt();
            if (rawBatvolt != null) {
                data.setBatvolt(rawBatvolt / 1000.0); // mV转V：原始值(mV) ÷ 1000 = 电压(V)
                log.debug("🔄 REST API辐射设备电压转换: 设备={}, 原始值={}mV, 转换后值={}V",
                    request.getDeviceCode(), rawBatvolt, data.getBatvolt());
            }
            data.setTime(request.getTime());
            data.setRecordTime(LocalDateTime.now());
            data.setDataTrigger(request.getTrigger());
            data.setMulti(request.getMulti());
            data.setWay(request.getWay());

            // GPS数据选择（根据useful字段自动选择BDS或LBS）
            if (request.getBdsUseful() != null && request.getBdsUseful() == 1
                && request.getBdsLongitude() != null && request.getBdsLatitude() != null) {
                // BDS可用，使用北斗
                data.setGpsType("BDS");
                data.setGpsLongitude(request.getBdsLongitude());
                data.setGpsLatitude(request.getBdsLatitude());
                data.setGpsUtc(request.getBdsUtc());
            } else if (request.getLbsLongitude() != null && request.getLbsLatitude() != null) {
                // BDS不可用，使用LBS
                data.setGpsType("LBS");
                data.setGpsLongitude(request.getLbsLongitude());
                data.setGpsLatitude(request.getLbsLatitude());
                data.setGpsUtc(null);
            }

            RadiationDeviceData savedData = radiationDeviceDataService.save(data);

            // SSE推送实时数据
            try {
                DeviceDataEvent event = new DeviceDataEvent(
                    "radiation-data",
                    request.getDeviceCode(),
                    "RADIATION_MONITOR",
                    java.util.Map.of(
                        "cpm", savedData.getCpm(),
                        "batVolt", savedData.getBatvolt(),
                        "recordTime", savedData.getRecordTime().toString()
                    )
                );
                sseEmitterService.broadcastDeviceData(device.getCompany().getId(), event);
                log.debug("📡 SSE推送辐射数据成功: {}", request.getDeviceCode());
            } catch (Exception e) {
                log.warn("⚠️ SSE推送辐射数据失败: {}", e.getMessage());
            }

            DeviceDataReceiveResponse response = DeviceDataReceiveResponse.builder()
                    .success(true)
                    .message("数据接收成功")
                    .deviceId(savedData.getId())
                    .deviceCode(request.getDeviceCode())
                    .receiveTime(LocalDateTime.now())
                    .build();

            log.info("辐射设备数据接收成功 - 设备: {}, 数据ID: {}", request.getDeviceCode(), savedData.getId());
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("处理辐射设备数据时发生错误 - 设备: {}", request.getDeviceCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("数据处理失败: " + e.getMessage()));
        }
    }

    /**
     * 接收环境设备数据
     */
    @PostMapping("/environment")
    public ResponseEntity<ApiResponse<DeviceDataReceiveResponse>> receiveEnvironmentData(
            @Valid @RequestBody EnvironmentDataReceiveRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        log.info("接收到环境设备数据上报 - 设备编码: {}, IP: {}", request.getDeviceCode(), clientIp);

        try {
            // 验证设备是否存在
            Device device = deviceService.findByDeviceCode(request.getDeviceCode());
            if (device == null) {
                log.warn("设备不存在: {}", request.getDeviceCode());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("设备不存在: " + request.getDeviceCode()));
            }

            // 更新设备最后在线时间
            device.setLastOnlineAt(LocalDateTime.now());
            deviceService.updateDevice(device.getId(), device, device.getCompany().getId());

            // 创建环境设备数据记录
            EnvironmentDeviceData data = new EnvironmentDeviceData();
            data.setDeviceCode(request.getDeviceCode());
            data.setRawData(request.getRawData());
            data.setSrc(request.getSrc());

            // 应用CPM转换系数
            Double rawCpm = request.getCpm();
            if (rawCpm != null) {
                double convertedCpm = cpmConversionProperties.isEnabled()
                    ? rawCpm / cpmConversionProperties.getEnvironmentConversionFactor()
                    : rawCpm;
                data.setCpm(convertedCpm);
                if (cpmConversionProperties.isEnabled()) {
                    log.debug("🔄 REST API环境设备CPM转换: 设备={}, 原始值={}, 转换系数={}, 转换后值={}",
                        request.getDeviceCode(), rawCpm,
                        cpmConversionProperties.getEnvironmentConversionFactor(), convertedCpm);
                }
            }

            data.setTemperature(request.getTemperature());
            data.setWetness(request.getWetness());
            data.setWindspeed(request.getWindspeed());
            data.setTotal(request.getTotal());
            data.setBattery(request.getBattery());
            data.setRecordTime(LocalDateTime.now());

            EnvironmentDeviceData savedData = environmentDeviceDataService.save(data);

            // SSE推送实时数据
            try {
                DeviceDataEvent event = new DeviceDataEvent(
                    "environment-data",
                    request.getDeviceCode(),
                    "ENVIRONMENT_STATION",
                    java.util.Map.of(
                        "cpm", savedData.getCpm(),
                        "temperature", savedData.getTemperature(),
                        "wetness", savedData.getWetness(),
                        "windspeed", savedData.getWindspeed(),
                        "recordTime", savedData.getRecordTime().toString()
                    )
                );
                sseEmitterService.broadcastDeviceData(device.getCompany().getId(), event);
                log.debug("📡 SSE推送环境数据成功: {}", request.getDeviceCode());
            } catch (Exception e) {
                log.warn("⚠️ SSE推送环境数据失败: {}", e.getMessage());
            }

            DeviceDataReceiveResponse response = DeviceDataReceiveResponse.builder()
                    .success(true)
                    .message("数据接收成功")
                    .deviceId(savedData.getId())
                    .deviceCode(request.getDeviceCode())
                    .receiveTime(LocalDateTime.now())
                    .build();

            log.info("环境设备数据接收成功 - 设备: {}, 数据ID: {}", request.getDeviceCode(), savedData.getId());
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("处理环境设备数据时发生错误 - 设备: {}", request.getDeviceCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("数据处理失败: " + e.getMessage()));
        }
    }

    /**
     * 批量接收辐射设备数据
     */
    @PostMapping("/radiation/batch")
    public ResponseEntity<ApiResponse<List<DeviceDataReceiveResponse>>> receiveRadiationDataBatch(
            @Valid @RequestBody List<RadiationDataReceiveRequest> requests,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        log.info("接收到批量辐射设备数据上报 - 数据条数: {}, IP: {}", requests.size(), clientIp);

        List<DeviceDataReceiveResponse> responses = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (RadiationDataReceiveRequest request : requests) {
            try {
                ResponseEntity<ApiResponse<DeviceDataReceiveResponse>> response =
                        receiveRadiationData(request, httpRequest);

                if (response.getStatusCode().is2xxSuccessful()) {
                    responses.add(response.getBody().getData());
                    successCount++;
                } else {
                    failureCount++;
                    log.warn("批量数据中的单条数据处理失败 - 设备: {}", request.getDeviceCode());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("批量数据处理时发生错误 - 设备: {}", request.getDeviceCode(), e);
            }
        }

        log.info("批量辐射设备数据处理完成 - 成功: {}, 失败: {}", successCount, failureCount);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 批量接收环境设备数据
     */
    @PostMapping("/environment/batch")
    public ResponseEntity<ApiResponse<List<DeviceDataReceiveResponse>>> receiveEnvironmentDataBatch(
            @Valid @RequestBody List<EnvironmentDataReceiveRequest> requests,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        log.info("接收到批量环境设备数据上报 - 数据条数: {}, IP: {}", requests.size(), clientIp);

        List<DeviceDataReceiveResponse> responses = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (EnvironmentDataReceiveRequest request : requests) {
            try {
                ResponseEntity<ApiResponse<DeviceDataReceiveResponse>> response =
                        receiveEnvironmentData(request, httpRequest);

                if (response.getStatusCode().is2xxSuccessful()) {
                    responses.add(response.getBody().getData());
                    successCount++;
                } else {
                    failureCount++;
                    log.warn("批量数据中的单条数据处理失败 - 设备: {}", request.getDeviceCode());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("批量数据处理时发生错误 - 设备: {}", request.getDeviceCode(), e);
            }
        }

        log.info("批量环境设备数据处理完成 - 成功: {}, 失败: {}", successCount, failureCount);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}