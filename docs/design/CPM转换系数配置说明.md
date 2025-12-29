# CPM和电压转换说明

## 问题描述

### 1. CPM值转换
不同类型的设备发送的CPM（每分钟计数）值使用不同的单位，需要在入库时进行转换：

- **辐射设备（RADIATION）**: 原始CPM值需要 **除以 10** 才能得到标准CPM值
- **环境设备（ENVIRONMENT）**: 原始CPM值需要 **除以 634** 才能得到标准CPM值

### 2. 电压值转换
辐射设备发送的电池电压单位是毫伏(mV)，需要转换为伏(V)存储：

- **辐射设备（RADIATION）**: 原始电压值需要 **除以 1000** 转换为伏(V)

## 为什么需要转换？

### 辐射设备CPM ÷10
辐射设备可能报告的是"每10秒的计数"或使用特定的传感器量程，因此需要除以10转换为标准CPM（每分钟计数）。

### 环境设备CPM ÷634
634很可能是该型号环境辐射传感器的特定校准系数，与传感器灵敏度、测量范围等因素相关。

### 辐射设备电压 ÷1000
辐射设备硬件报告的电池电压单位是毫伏(mV)，而数据库和国际标准单位是伏(V)，因此需要除以1000进行转换。

## 实现方案

### 1. 配置类 `CpmConversionProperties`

位置: `backend/src/main/java/com/cdutetc/ems/config/CpmConversionProperties.java`

```java
@ConfigurationProperties(prefix = "app.ems.mqtt.cpm")
public class CpmConversionProperties {
    private double radiationConversionFactor = 10.0;      // 辐射设备转换系数
    private double environmentConversionFactor = 634.0;   // 环境设备转换系数
    private boolean enabled = true;                        // 是否启用转换
}
```

### 2. 配置文件 `application.yaml`

```yaml
app:
  ems:
    mqtt:
      cpm:
        enabled: true
        radiation-conversion-factor: ${EMS_CPM_RADIATION_FACTOR:10.0}
        environment-conversion-factor: ${EMS_CPM_ENVIRONMENT_FACTOR:634.0}
```

### 3. 转换逻辑

#### MQTT消息监听器 (`MqttMessageListener.java`)

**辐射设备CPM转换**:
```java
JsonParserUtil.parseDouble(rootNode, "CPM").ifPresent(rawCpm -> {
    double convertedCpm = cpmConversionProperties.isEnabled()
        ? rawCpm / cpmConversionProperties.getRadiationConversionFactor()
        : rawCpm;
    data.setCpm(convertedCpm);
    log.debug("🔄 辐射设备CPM转换: 原始值={}, 转换系数={}, 转换后值={}",
        rawCpm, cpmConversionProperties.getRadiationConversionFactor(), convertedCpm);
});
```

**环境设备CPM转换**:
```java
JsonParserUtil.parseDouble(rootNode, "CPM").ifPresent(rawCpm -> {
    double convertedCpm = cpmConversionProperties.isEnabled()
        ? rawCpm / cpmConversionProperties.getEnvironmentConversionFactor()
        : rawCpm;
    data.setCpm(convertedCpm);
    log.debug("🔄 环境设备CPM转换: 原始值={}, 转换系数={}, 转换后值={}",
        rawCpm, cpmConversionProperties.getEnvironmentConversionFactor(), convertedCpm);
});
```

**辐射设备电压转换** (硬编码，不可配置):
```java
// 辐射设备发送的是毫伏mV，需要转换为伏V存储
JsonParserUtil.parseDouble(rootNode, "Batvolt").ifPresent(rawBatvolt -> {
    data.setBatvolt(rawBatvolt / 1000.0); // mV转V：原始值(mV) ÷ 1000 = 电压(V)
    log.debug("🔄 辐射设备电压转换: 原始值={}mV, 转换后值={}V",
        rawBatvolt, data.getBatvolt());
});
```

#### REST API控制器 (`DeviceDataReceiverController.java`)

同样的转换逻辑也应用于REST API接收的设备数据：

- `/api/device-data/radiation` - 辐射设备数据接收
- `/api/device-data/environment` - 环境设备数据接收
- `/api/device-data/radiation/batch` - 批量辐射设备数据
- `/api/device-data/environment/batch` - 批量环境设备数据

## 配置方法

### 方式1: 修改 `application.yaml`

直接编辑配置文件修改默认值：

```yaml
app:
  ems:
    mqtt:
      cpm:
        enabled: true
        radiation-conversion-factor: 10.0
        environment-conversion-factor: 634.0
```

### 方式2: 使用环境变量（Docker部署）

```bash
# 辐射设备CPM转换系数
export EMS_CPM_RADIATION_FACTOR=10.0

# 环境设备CPM转换系数
export EMS_CPM_ENVIRONMENT_FACTOR=634.0
```

或在 `docker-compose.yml` 中配置：

```yaml
services:
  ems-backend:
    environment:
      - EMS_CPM_RADIATION_FACTOR=10.0
      - EMS_CPM_ENVIRONMENT_FACTOR=634.0
```

### 方式3: 禁用CPM转换

如果硬件已经发送标准CPM值，可以禁用转换：

```yaml
app:
  ems:
    mqtt:
      cpm:
        enabled: false  # 禁用转换，直接使用原始值
```

## 使用示例

### 示例1: 辐射设备

假设辐射设备发送原始数据：
```json
{
  "CPM": 120,
  "Batvolt": 3700
}
```

**CPM转换**:
- **转换前**: 120（原始值）
- **转换系数**: 10
- **转换后**: 120 ÷ 10 = 12.0 CPM（标准值）

**电压转换**:
- **转换前**: 3700 mV（原始值）
- **转换后**: 3700 ÷ 1000 = 3.7 V（标准值）

**数据库中存储**: `CPM=12.0, Batvolt=3.7`

### 示例2: 环境设备

假设环境设备发送原始数据：
```json
{
  "CPM": 2536,
  "battery": 3.6
}
```

**CPM转换**:
- **转换前**: 2536（原始值）
- **转换系数**: 634
- **转换后**: 2536 ÷ 634 = 4.0 CPM（标准值）

**电压**: 环境设备的battery字段已经是伏(V)单位，不需要转换

**数据库中存储**: `CPM=4.0, battery=3.6`

## 调试日志

系统会记录详细的CPM和电压转换日志（DEBUG级别）：

```
🔄 辐射设备CPM转换: 原始值=120, 转换系数=10.0, 转换后值=12.0
🔄 辐射设备电压转换: 原始值=3700mV, 转换后值=3.7V
🔄 环境设备CPM转换: 原始值=2536, 转换系数=634.0, 转换后值=4.0
🔄 REST API辐射设备CPM转换: 设备=GM5-TEST001, 原始值=150, 转换系数=10.0, 转换后值=15.0
🔄 REST API辐射设备电压转换: 设备=GM5-TEST001, 原始值=3800mV, 转换后值=3.8V
```

## 修改转换系数的步骤

1. **确定新的转换系数**: 与硬件团队确认正确的转换值
2. **修改配置**: 更新 `application.yaml` 或设置环境变量
3. **重启应用**: 使配置生效
4. **验证数据**: 检查数据库中的CPM值是否符合预期

## 注意事项

1. **原始数据保留**: MQTT和REST API接收的**原始JSON数据**保存在 `raw_data` 字段中，不受转换影响，可用于事后分析
2. **转换时机**: CPM转换在数据入库前完成，数据库中存储的是转换后的标准CPM值
3. **历史数据**: 配置修改只影响**新接收的数据**，不会转换历史数据
4. **测试验证**: 修改转换系数后，建议先在测试环境验证，确认CPM值合理后再应用到生产环境

## 常见问题

### Q1: 为什么辐射设备是 ÷10 而环境设备是 ÷634？

A: 这是硬件传感器特性决定的：
- 辐射设备可能报告的是10秒计数或其他单位
- 环境设备的634是该型号传感器的特定校准系数

### Q2: 如何确认转换系数是否正确？

A: 可以通过以下方式验证：
1. 对比设备显示屏的CPM值与系统记录的值
2. 查看原始数据 `raw_data` 字段，手动计算验证
3. 与硬件团队确认传感器的输出规格

### Q3: 禁用转换后会发生什么？

A: 禁用转换（`enabled: false`）后，系统会直接使用设备发送的原始CPM值，不进行任何转换。

### Q4: 如何批量修正历史数据？

A: 如果需要修正历史数据的CPM值，需要编写SQL脚本直接更新数据库。建议先备份数据库，并在测试环境验证。

```sql
-- 示例：将历史辐射设备CPM值除以10（当前值是原始值的情况）
UPDATE radiation_device_data
SET cpm = cpm / 10.0
WHERE device_code LIKE 'GM5%';
```
