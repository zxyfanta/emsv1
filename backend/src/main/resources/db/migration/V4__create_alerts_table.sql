-- 告警信息表
-- 用于存储设备告警信息
CREATE TABLE alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型: HIGH_CPM, OFFLINE, FAULT, LOW_BATTERY',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度: CRITICAL, WARNING, INFO',
    device_code VARCHAR(50) COMMENT '相关设备编码',
    device_id BIGINT COMMENT '相关设备ID',
    company_id BIGINT NOT NULL COMMENT '所属企业',
    message TEXT NOT NULL COMMENT '告警消息',
    data JSON COMMENT '告警详细数据（CPM值等）',
    resolved BOOLEAN DEFAULT FALSE COMMENT '是否已解决',
    resolved_at TIMESTAMP NULL COMMENT '解决时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_company_created (company_id, created_at),
    INDEX idx_device (device_id),
    INDEX idx_resolved (resolved),
    INDEX idx_alert_type (alert_type),
    INDEX idx_severity (severity)
) COMMENT '告警信息表';
