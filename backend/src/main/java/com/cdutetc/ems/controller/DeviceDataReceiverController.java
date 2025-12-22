package com.cdutetc.ems.controller;

import com.cdutetc.ems.util.ApiResponse;
import com.cdutetc.ems.dto.request.RadiationDataReceiveRequest;
import com.cdutetc.ems.dto.request.EnvironmentDataReceiveRequest;
import com.cdutetc.ems.dto.response.DeviceDataReceiveResponse;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.service.DeviceService;
import com.cdutetc.ems.service.RadiationDeviceDataService;
import com.cdutetc.ems.service.EnvironmentDeviceDataService;
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
            data.setCpm(request.getCpm());
            data.setBatvolt(request.getBatvolt());
            data.setTime(request.getTime());
            data.setRecordTime(LocalDateTime.now());
            data.setTrigger(request.getTrigger());
            data.setMulti(request.getMulti());
            data.setWay(request.getWay());

            // 位置数据
            data.setBdsLongitude(request.getBdsLongitude());
            data.setBdsLatitude(request.getBdsLatitude());
            data.setBdsUtc(request.getBdsUtc());
            data.setLbsLongitude(request.getLbsLongitude());
            data.setLbsLatitude(request.getLbsLatitude());
            data.setLbsUseful(request.getLbsUseful());

            RadiationDeviceData savedData = radiationDeviceDataService.save(data);

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
            data.setCpm(request.getCpm());
            data.setTemperature(request.getTemperature());
            data.setWetness(request.getWetness());
            data.setWindspeed(request.getWindspeed());
            data.setTotal(request.getTotal());
            data.setBattery(request.getBattery());
            data.setRecordTime(LocalDateTime.now());

            EnvironmentDeviceData savedData = environmentDeviceDataService.save(data);

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