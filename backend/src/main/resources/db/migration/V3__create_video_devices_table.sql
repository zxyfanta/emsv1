-- 视频设备表
-- 用于管理第三方视频流设备，支持绑定到监测设备
CREATE TABLE video_devices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_code VARCHAR(50) UNIQUE NOT NULL COMMENT '视频设备编码',
    device_name VARCHAR(100) COMMENT '视频设备名称',
    stream_url VARCHAR(500) COMMENT '第三方视频流URL (RTSP/RTMP/HLS/FLV)',
    stream_type VARCHAR(20) COMMENT '流类型: RTSP, RTMP, HLS, FLV, WEBRTC',
    snapshot_url VARCHAR(500) COMMENT '视频截图URL',
    username VARCHAR(100) COMMENT '视频流认证用户名（可选）',
    password VARCHAR(100) COMMENT '视频流认证密码（可选）',
    resolution VARCHAR(20) COMMENT '分辨率: 1920x1080',
    fps INT COMMENT '帧率',
    status VARCHAR(20) DEFAULT 'OFFLINE' COMMENT '状态: ONLINE, OFFLINE',
    linked_device_id BIGINT COMMENT '绑定的监测设备ID',
    company_id BIGINT NOT NULL COMMENT '所属企业',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (linked_device_id) REFERENCES devices(id) ON DELETE SET NULL,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_company (company_id),
    INDEX idx_linked_device (linked_device_id),
    INDEX idx_status (status)
) COMMENT '视频设备管理表';
