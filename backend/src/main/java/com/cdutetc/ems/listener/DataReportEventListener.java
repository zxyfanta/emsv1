package com.cdutetc.ems.listener;

import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.cdutetc.ems.service.report.DataReportRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据上报事件监听器
 * 监听设备数据接收事件，触发数据上报
 * 修复：使用@EventListener替代@TransactionalEventListener，避免事务依赖问题
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataReportEventListener {

    private final DataReportRouterService routerService;

    /**
     * 监听设备数据接收事件
     * 使用@EventListener而非@TransactionalEventListener，因为：
     * 1. MQTT监听器不在事务上下文中
     * 2. 数据已通过@Transactional服务保存
     * 3. reportAsync方法本身是异步的，有独立的错误处理
     *
     * @param event 设备数据事件
     */
    @EventListener(classes = DeviceDataEvent.class)
    public void handleDeviceDataReceivedEvent(DeviceDataEvent event) {
        try {
            String eventType = event.getEventType();
            String deviceType = event.getDeviceType();

            log.info("📨 收到设备数据事件: eventType={}, deviceType={}, deviceCode={}",
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
