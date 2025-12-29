# GPS数据处理优化方案

## 📋 背景和目标

### 原有问题
- 设备通过MQTT上报两组GPS数据：BDS（北斗）和LBS（基站）
- 每组GPS数据包含：经度、纬度、`useful`标志（1=可用）
- 系统需要根据`useful`字段智能选择GPS数据
- 设备配置中的`gpsPriority`字段手动配置，不够灵活

### 优化目标
✅ 在数据接收时自动选择最优GPS（根据`useful`字段）
✅ 统一存储格式，简化数据库结构
✅ 上报时无需判断GPS类型，直接使用
✅ 移除不必要的配置项（`gpsPriority`）

---

## 🎯 GPS选择逻辑

### 核心规则
```
IF BDS.useful == 1 AND BDS.longitude != null AND BDS.latitude != null THEN
    使用 BDS（北斗GPS）
ELSE IF LBS.longitude != null AND LBS.latitude != null THEN
    使用 LBS（基站GPS）
ELSE
    GPS数据缺失，不存储GPS信息
END IF
```

### 实现位置
- **MQTT数据接收**：[MqttMessageListener.java:430-468](backend/src/main/java/com/cdutetc/ems/mqtt/MqttMessageListener.java#L430-L468)
- **REST API数据接收**：[DeviceDataReceiverController.java:102-116](backend/src/main/java/com/cdutetc/ems/controller/DeviceDataReceiverController.java#L102-L116)

---

## 📊 数据库Schema变更

### 删除的字段（7个）
```sql
-- 从 ems_radiation_device_data 表删除
BDS_longitude VARCHAR(50)    -- 北斗经度
BDS_latitude VARCHAR(50)     -- 北斗纬度
BDS_utc VARCHAR(50)          -- 北斗UTC时间
BDS_useful INT               -- 北斗可用标志
LBS_longitude VARCHAR(50)    -- 基站经度
LBS_latitude VARCHAR(50)     -- 基站纬度
LBS_useful INT               -- 基站可用标志
```

### 新增的字段（4个）
```sql
-- 新增到 ems_radiation_device_data 表
gps_longitude VARCHAR(50)  COMMENT '经度（度分格式：DDMM.MMMM）'
gps_latitude VARCHAR(50)   COMMENT '纬度（度分格式：DDMM.MMMM）'
gps_type VARCHAR(20)       COMMENT 'GPS类型：BDS=北斗，LBS=基站'
gps_utc VARCHAR(50)        COMMENT 'GPS UTC时间（仅BDS有值）'
```

### 删除的配置字段（1个）
```sql
-- 从 ems_device 表删除
gps_priority VARCHAR(20)    -- GPS优先级（BDS/LBS/BDS_THEN_LBS）
```

### 验证SQL
```sql
-- 检查新字段是否创建成功
DESC ems_radiation_device_data;

-- 预期输出应包含：
-- gps_latitude    varchar(50)    YES
-- gps_longitude   varchar(50)    YES
-- gps_type        varchar(20)    YES
-- gps_utc         varchar(50)    YES

-- 检查旧字段是否删除
DESC ems_device;

-- 预期输出不应包含：
-- gps_priority
```

---

## 🔧 代码变更清单

### 1. 实体类和DTO（6个文件）

#### [RadiationDeviceData.java](backend/src/main/java/com/cdutetc/ems/entity/RadiationDeviceData.java)
- ❌ 删除：`bdsLongitude`, `bdsLatitude`, `bdsUtc`, `bdsUseful`, `lbsLongitude`, `lbsLatitude`, `lbsUseful`
- ✅ 新增：`gpsLongitude`, `gpsLatitude`, `gpsType`, `gpsUtc`

#### [Device.java](backend/src/main/java/com/cdutetc/ems/entity/Device.java)
- ❌ 删除：`gpsPriority`字段及其getter/setter

#### [DeviceReportConfig.java](backend/src/main/java/com/cdutetc/ems/dto/DeviceReportConfig.java)
- ❌ 删除：`gpsPriority`字段
- ❌ 删除：`fromDevice()`方法中的`setGpsPriority()`调用

#### [DeviceResponse.java](backend/src/main/java/com/cdutetc/ems/dto/response/DeviceResponse.java)
- ❌ 删除：`gpsPriority`字段声明
- ❌ 删除：`fromDevice()`方法中的`.gpsPriority()`调用

#### [RadiationDeviceDataResponse.java](backend/src/main/java/com/cdutetc/ems/dto/response/RadiationDeviceDataResponse.java)
- ❌ 删除：`bdsLongitude`, `bdsLatitude`, `bdsUtc`, `bdsUseful`, `lbsLongitude`, `lbsLatitude`, `lbsUseful`
- ✅ 新增：`gpsType`, `gpsLongitude`, `gpsLatitude`, `gpsUtc`

#### [RadiationDataReceiveRequest.java](backend/src/main/java/com/cdutetc/ems/dto/request/RadiationDataReceiveRequest.java)
- ✅ 新增：`bdsUseful`字段（用于接收MQTT数据）

### 2. 数据接收层（2个文件）

#### [MqttMessageListener.java](backend/src/main/java/com/cdutetc/ems/mqtt/MqttMessageListener.java)
- ✅ 新增：`selectGpsData()`方法（430-468行）
- ✅ 新增：`GpsData`内部类（470-480行）
- ✅ 修改：`handleRadiationData()`方法，使用GPS选择逻辑（214-223行）

#### [DeviceDataReceiverController.java](backend/src/main/java/com/cdutetc/ems/controller/DeviceDataReceiverController.java)
- ✅ 修改：GPS数据设置逻辑（102-116行）
- ✅ 实现：根据`useful`字段选择GPS

### 3. 数据上报服务（2个文件）

#### [SichuanDataReportService.java](backend/src/main/java/com/cdutetc/ems/service/report/SichuanDataReportService.java)
- ✅ 简化：`buildDataStr()`方法，直接使用`gps_*`字段（144-157行）
- ❌ 删除：`determineGPS()`, `putLBSCoordinates()`, `formatCoordinate()`方法

#### [ShandongDataReportService.java](backend/src/main/java/com/cdutetc/ems/service/report/ShandongDataReportService.java)
- ✅ 简化：`buildHJT212Data()`方法，直接使用`gps_*`字段（123-146行）
- ❌ 删除：`determineGPSFlag()`, `determineLongitude()`, `determineLatitude()`, `formatCoordinateToHJT212()`方法

### 4. 配置和业务逻辑（2个文件）

#### [DeviceService.java](backend/src/main/java/com/cdutetc/ems/service/DeviceService.java)
- ❌ 删除：`updateDevice()`方法中的`gpsPriority`更新逻辑（131-133行）

#### [DataInitializer.java](backend/src/main/java/com/cdutetc/ems/config/DataInitializer.java)
- ✅ 修改：测试数据生成逻辑，使用新的GPS字段（366-370行）

---

## 🧪 测试验证

### GPS选择逻辑测试（4个测试用例）

所有测试用例通过 ✅

```
✅ 测试通过: BDS可用时正确选择BDS
   - 输入：BDS.useful=1, LBS数据完整
   - 预期：选择BDS
   - 结果：PASS

✅ 测试通过: BDS不可用时正确选择LBS
   - 输入：BDS.useful=0, LBS数据完整
   - 预期：选择LBS
   - 结果：PASS

✅ 测试通过: BDS useful为null时正确选择LBS
   - 输入：BDS.useful=null, LBS数据完整
   - 预期：选择LBS
   - 结果：PASS

✅ 测试通过: GPS都不可用时正确返回null
   - 输入：BDS.useful=0, LBS数据缺失
   - 预期：返回null
   - 结果：PASS
```

### 数据库验证

```bash
# 验证新字段创建
docker exec ems-mysql mysql -uems_user -pems_pass ems_db \
  -e "DESC ems_radiation_device_data;" | grep -i gps

# 预期输出：
# gps_latitude    varchar(50)    YES
# gps_longitude   varchar(50)    YES
# gps_type        varchar(20)    YES
# gps_utc         varchar(50)    YES

# 验证旧字段删除
docker exec ems-mysql mysql -uems_user -pems_pass ems_db \
  -e "DESC ems_radiation_device_data;" | grep -iE "(bds|lbs)"

# 预期输出：（空）
```

---

## 📝 前端配置需求更新

### 需要移除的配置项
- ❌ **GPS优先级**（gpsPriority）- 不再需要手动配置

### 需要调整的显示
- **设备详情页**：GPS数据从分开的BDS/LBS变为统一显示
- **GPS类型标识**：新增`gps_type`字段显示（BDS/LBS）
- **数据响应格式**：使用新的字段名（`gpsType`, `gpsLongitude`, `gpsLatitude`, `gpsUtc`）

---

## 🎉 实现效果

### 数据流简化
```
旧流程：
设备数据 → 解析BDS和LBS → 存储两组GPS → 根据gpsPriority选择 → 上报

新流程：
设备数据 → 根据useful选择GPS → 存储单一GPS → 直接上报
```

### 优势
1. ✅ **自动化**：无需手动配置GPS优先级，系统自动选择最优GPS
2. ✅ **简化存储**：数据库只存储有效的GPS数据
3. ✅ **代码简洁**：上报逻辑简化，无需判断GPS类型
4. ✅ **符合需求**：完全满足甲方要求（根据useful字段选择）

---

## 🔄 迁移步骤

### 开发环境
1. ✅ 修改实体类和DTO
2. ✅ 实现GPS选择逻辑
3. ✅ 更新上报服务
4. ✅ 创建并运行测试
5. ✅ 验证数据库Schema

### 生产环境（建议）
1. **备份数据**：`mysqldump ems_db > backup.sql`
2. **停止服务**：确保没有新数据写入
3. **数据迁移**：执行SQL脚本迁移历史数据（可选）
4. **部署代码**：发布新版本
5. **验证功能**：测试GPS选择和上报功能

---

## 📌 注意事项

### raw_data字段
- `raw_data`字段仍保留完整的原始JSON（包含BDS和LBS数据）
- 如需事后分析GPS选择逻辑，可从`raw_data`中提取原始数据

### GPS坐标格式
- 设备上报的GPS坐标已经是度分格式（DDMM.MMMM）
- 系统直接存储，无需转换

### 兼容性
- REST API请求仍然接收BDS和LBS数据（向后兼容）
- 内部自动选择最优GPS存储

---

## 📚 相关文档

- [数据上报模块设计方案V3-极简版](./数据上报模块设计方案V3-极简版.md)
- [双上报模式综合设计方案V2-极简版](./双上报模式综合设计方案V2-极简版.md)
- [数据上报模块实施计划](./数据上报模块实施计划.md)

---

**文档版本**：1.0
**创建日期**：2025-12-29
**最后更新**：2025-12-29
**作者**：Claude Code
**状态**：✅ 已实现并验证
