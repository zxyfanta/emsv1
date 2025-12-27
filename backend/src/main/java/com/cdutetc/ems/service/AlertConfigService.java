package com.cdutetc.ems.service;

import com.cdutetc.ems.config.AlertProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 告警配置服务（简化版）
 * 所有企业使用相同的全局配置
 *
 * @author EMS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertConfigService {

    private final AlertProperties alertProperties;

    /**
     * 获取CPM上升率配置
     */
    public AlertProperties.CpmRise getCpmRiseConfig() {
        return alertProperties.getCpmRise();
    }

    /**
     * 获取低电压配置
     */
    public AlertProperties.LowBattery getLowBatteryConfig() {
        return alertProperties.getLowBattery();
    }

    /**
     * 获取离线超时配置
     */
    public AlertProperties.OfflineTimeout getOfflineTimeoutConfig() {
        return alertProperties.getOfflineTimeout();
    }
}
