-- ==========================================
-- 山东协议测试设备配置SQL
-- SIM卡号: 865229085145869
-- 创建日期: 2024-12-29
-- ==========================================

-- 步骤1: 确保企业存在（使用默认企业ID=1）
-- 注意：如果企业不存在，需要先创建企业
INSERT IGNORE INTO ems_company (id, company_name, contact_email, contact_phone, address, status, created_at, updated_at)
VALUES (1, '默认测试企业', 'test@example.com', '13800138000', '测试地址', 'ACTIVE', NOW(), NOW());

-- 步骤2: 创建山东协议测试设备（辐射设备）
-- SIM卡号作为设备编码
INSERT INTO ems_device (
    device_code,
    device_name,
    device_type,
    company_id,
    status,
    activation_status,
    description,
    location,
    manufacturer,
    model,
    serial_number,

    -- 辐射设备上报专用字段（山东协议）
    nuclide,
    inspection_machine_number,      -- 探伤机编号 (Ma字段)
    source_number,                   -- 放射源编号 (Rno字段)
    source_type,                     -- 放射源类型 (Xtype: 01=I类, 02=II类, 03=III类, 04=IV类, 05=V类)
    original_activity,               -- 原始活度 (LastAct: 科学计数法，如 5.530E012)
    current_activity,                -- 当前活度 (NowAct: 科学计数法，如 3.270E012)
    source_production_date,          -- 放射源出厂日期 (SourceTime: yyyyMMdd)

    -- 数据上报配置
    data_report_enabled,
    report_protocol,

    -- 审计字段
    created_at,
    updated_at
) VALUES (
    -- 基本信息
    '865229085145869',               -- SIM卡号 = 设备编号(MN)
    '山东协议测试辐射设备',
    'RADIATION_MONITOR',
    1,                               -- 归属企业ID
    'ONLINE',
    'ACTIVE',                        -- 已激活状态（重要：未激活设备数据会被丢弃）
    '用于测试山东协议数据上报的辐射设备',
    '山东省烟台市测试地点',
    '测试设备厂',
    'RAD-TEST-001',
    'SN865229085145869',

    -- 辐射设备专用字段（根据协议文档配置）
    'Cs-137',                        -- 核素
    '002162',                        -- 探伤机编号 (Ma)
    'DE25IR006722',                  -- 放射源编号 (Rno) - 可以包含字母
    '02',                            -- 放射源类型：II类
    '5.530E012',                     -- 原始活度：5.53 × 10^12 Bq
    '3.270E012',                     -- 当前活度：3.27 × 10^12 Bq
    '2025-07-03',                    -- 放射源出厂日期

    -- 上报配置
    true,                            -- 启用数据上报
    'SHANDONG',                      -- 使用山东协议

    -- 审计字段
    NOW(),
    NOW()
);

-- 步骤3: 验证设备创建结果
SELECT
    id,
    device_code AS 'SIM卡号',
    device_name AS '设备名称',
    device_type AS '设备类型',
    activation_status AS '激活状态',
    data_report_enabled AS '上报启用',
    report_protocol AS '上报协议',
    inspection_machine_number AS '探伤机编号',
    source_number AS '放射源编号',
    source_type AS '源类型',
    original_activity AS '原始活度',
    current_activity AS '当前活度',
    source_production_date AS '出厂日期',
    created_at AS '创建时间'
FROM ems_device
WHERE device_code = '865229085145869';

-- ==========================================
-- 字段说明
-- ==========================================
-- device_code (SIM卡号): 865229085145869
--   - MQTT主题: ems/device/865229085145869/data/RADIATION
--   - HJ/T212 MN字段: 865229085145869
--
-- inspection_machine_number (探伤机编号): 002162
--   - HJ/T212 Ma字段
--
-- source_number (放射源编号): DE25IR006722
--   - HJ/T212 Rno字段
--   - 可以包含字母（用户确认）
--
-- source_type (放射源类型): 02
--   - HJ/T212 Xtype字段
--   - 01=I类, 02=II类, 03=III类, 04=IV类, 05=V类
--
-- original_activity (原始活度): 5.530E012
--   - HJ/T212 LastAct字段
--   - 科学计数法格式
--
-- current_activity (当前活度): 3.270E012
--   - HJ/T212 NowAct字段
--   - 科学计数法格式
--
-- source_production_date (出厂日期): 2025-07-03
--   - HJ/T212 SourceTime字段
--   - 格式: yyyyMMdd
--
-- report_protocol: SHANDONG
--   - 触发 ShandongDataReportService
--   - 协议: TCP + HJ/T212-2005
--   - 服务器: 221.214.62.118:20050
-- ==========================================
