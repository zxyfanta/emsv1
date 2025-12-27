package com.cdutetc.ems.listener;

import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.service.report.DataReportRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 数据上报事件监听器
 * 监听设备数据接收事件，异步触发数据上报
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataReportEventListener {

    private final DataReportRouterService routerService;

    /**
     * 监听设备数据接收事件
     * 在事务提交后执行，确保数据已保存
     *
     * @param event 设备数据事件
     */
    @Async("reportExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeviceDataReceivedEvent(DeviceDataEvent event) {
        try {
            String eventType = event.getEventType();
            String deviceType = event.getDeviceType();

            log.debug("📨 收到设备数据事件: eventType={}, deviceType={}, deviceCode={}",
                    eventType, deviceType, event.getDeviceCode());

            // 只处理辐射设备数据
            if ("radiation-data".equals(eventType) && "RADIATION_MONITOR".equals(deviceType)) {
                log.info("🎯 触发辐射设备上报: deviceCode={}", event.getDeviceCode());

                // 异步上报（注意：这里已经在异步线程中，但routerService.reportAsync方法本身也是@Async的）
                // Spring的@Async会嵌套执行，所以可以直接调用
                routerService.reportAsync(event.getDeviceCode(), event.getRadiationDeviceData());
            }

        } catch (Exception e) {
            log.error("❌ 处理数据上报事件失败: deviceCode={}, error={}",
                    event.getDeviceCode(), e.getMessage(), e);
            // 不抛出异常，避免影响主流程
        }
    }
}
