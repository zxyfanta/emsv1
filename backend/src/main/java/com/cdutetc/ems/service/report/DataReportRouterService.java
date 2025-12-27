package com.cdutetc.ems.service.report;

import com.cdutetc.ems.dto.DeviceReportConfig;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.entity.DataReportLog;
import com.cdutetc.ems.repository.DataReportLogRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.service.DeviceReportConfigCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 数据上报协议路由服务
 * 根据设备配置选择上报协议（四川/山东）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataReportRouterService {

    private final DeviceReportConfigCacheService cacheService;
    private final SichuanDataReportService sichuanService;
    private final ShandongDataReportService shandongService;
    private final DataReportLogRepository logRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 异步上报数据
     *
     * @param deviceCode 设备编码
     * @param data       辐射设备数据
     */
    @Async("reportExecutor")
    public void reportAsync(String deviceCode, RadiationDeviceData data) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("🚀 开始异步上报: deviceCode={}", deviceCode);

            // 1. 获取设备上报配置（从 Redis 或 MySQL）
            DeviceReportConfig config = cacheService.getReportConfig(deviceCode);

            // 2. 检查是否启用上报
            if (!Boolean.TRUE.equals(config.getDataReportEnabled())) {
                log.debug("⏭️ 设备未启用上报: deviceCode={}", deviceCode);
                return;
            }

            // 3. 根据协议路由
            String protocol = config.getReportProtocol();
            log.info("📡 路由上报: deviceCode={}, protocol={}", deviceCode, protocol);

            switch (protocol) {
                case "SICHUAN":
                    sichuanService.report(config, data);
                    break;

                case "SHANDONG":
                    shandongService.report(config, data);
                    break;

                default:
                    log.warn("⚠️ 未知的上报协议: deviceCode={}, protocol={}", deviceCode, protocol);
                    // 记录失败日志
                    saveReportLog(deviceCode, protocol, false, null, null,
                            "未知的上报协议: " + protocol, startTime);
                    return;
            }

            // 4. 记录成功日志（简化版，实际应该在上报服务中记录详细信息）
            saveReportLog(deviceCode, protocol, true, null, null, null, startTime);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 上报异常: deviceCode={}, 耗时={}ms, error={}",
                    deviceCode, duration, e.getMessage(), e);

            // 记录失败日志
            saveReportLog(deviceCode, "UNKNOWN", false, null, null,
                    e.getMessage(), startTime);
        }
    }

    /**
     * 保存上报日志
     *
     * @param deviceCode  设备编码
     * @param protocol    协议类型
     * @param status      状态
     * @param request     请求数据
     * @param response    响应数据
     * @param error       错误信息
     * @param startTime   开始时间
     */
    private void saveReportLog(String deviceCode, String protocol,
                               boolean status, String request, String response,
                               String error, long startTime) {
        try {
            // 获取设备ID
            Long deviceId = deviceRepository.findByDeviceCode(deviceCode)
                    .map(device -> device.getId())
                    .orElse(null);

            // 创建日志记录
            DataReportLog reportLog = DataReportLog.builder()
                    .deviceId(deviceId)
                    .deviceCode(deviceCode)
                    .reportProtocol(protocol)
                    .reportTime(LocalDateTime.now())
                    .requestPayload(request)
                    .responseBody(response)
                    .status(status ? "SUCCESS" : "FAILED")
                    .errorMessage(error)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

            logRepository.save(reportLog);
            log.debug("📝 上报日志已保存: deviceCode={}, status={}", deviceCode, status);

        } catch (Exception e) {
            log.warn("⚠️ 保存上报日志失败: deviceCode={}, error={}", deviceCode, e.getMessage());
        }
    }
}
