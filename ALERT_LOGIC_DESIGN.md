# 告警业务逻辑设计方案

生成时间：2025-12-27

## 📋 概述

本文档详细设计三项告警业务逻辑的实现方案，包括算法设计、数据流、边界条件处理和实现细节。

---

## 1️⃣ CPM上升率检查算法

### 业务需求

**背景**：CPM（Counts Per Minute）是一个累加值，会缓慢上升。当单次上升超过某百分比时触发告警。

**关键点**：
- CPM是累加值，不是瞬时值
- 需要检查上升率百分比，而非绝对值
- 避免频繁告警（最小检查间隔）
- 避免基数太小导致误报（最小CPM阈值）

### 算法设计

#### 核心公式
```
上升率 = (当前CPM - 上次CPM) / 上次CPM

判断条件：
- 上次CPM > 最小CPM基数（避免基数太小）
- 上升率 > 上升率阈值（如0.15，即15%）
- 距离上次告警时间 > 最小检查间隔（如5分钟）
```

#### 实现流程

```
MQTT消息到达
    ↓
解析CPM值
    ↓
从Redis获取上次CPM值
    ↓
判断条件检查
    ├─ 上次CPM是否存在？
    ├─ 上次CPM > minCpm？
    ├─ (current - last) / last > risePercentage？
    └─ 距离上次告警 > minInterval？
    ↓
满足所有条件 → 触发CPM_RISE告警
    ↓
更新告防重缓存（记录本次告警时间）
```

### 数据流设计

#### 使用Redis缓存的去重机制

**方案A**：使用Redis String存储上次告警时间
```
Key: alert:last:cpm-rise:{deviceCode}
Value: 上次告警时间戳（ISO格式）
TTL: minInterval + 60秒（自动过期）
```

**方案B**：使用DeviceStatusCache扩展字段
```
在device:status:{deviceCode} Hash中添加：
  - lastCpmRiseAlertAt: "2025-12-27T10:30:45"
```

**推荐**：方案B，避免额外Redis键，与设备状态集中管理。

### 边界条件处理

| 场景 | 处理方式 |
|------|----------|
| 首次启动，无历史CPM | 不触发告警，仅缓存当前值 |
| 上次CPM < minCpm | 不触发告警，避免基数太小误报 |
| CPM下降（如设备重启） | 上升率为负，不触发告警 |
| 短时间内多次超过阈值 | 使用告警去重缓存，5分钟内只告警一次 |
| Redis缓存丢失 | 从数据库查询历史CPM值（降级策略） |
| 设备被删除或停用 | 不触发告警，检查激活状态 |

### 配置参数

```yaml
app:
  ems:
    alert:
      cpm-rise:
        rise-percentage: 0.15    # 上升15%触发告警
        min-interval: 300        # 5分钟内不重复告警
        min-cpm: 50              # 最小CPM基数
```

### 实现伪代码

```java
public void checkCpmRiseAndAlert(String deviceCode, Double currentCpm,
                                  Long deviceId, Long companyId) {
    // 1. 获取配置
    var config = alertConfigService.getCpmRiseConfig();

    // 2. 从缓存获取上次CPM值
    Double lastCpm = cacheService.getLastCpm(deviceCode);

    // 3. 首次启动或无历史数据
    if (lastCpm == null) {
        log.debug("设备{}首次记录CPM值: {}", deviceCode, currentCpm);
        return;
    }

    // 4. 检查最小CPM基数
    if (lastCpm < config.getMinCpm()) {
        log.debug("设备{}上次CPM值{}低于最小基数{}，跳过检查",
                  deviceCode, lastCpm, config.getMinCpm());
        return;
    }

    // 5. 计算上升率
    double riseRate = (currentCpm - lastCpm) / lastCpm;

    // 6. 检查上升率是否超过阈值
    if (riseRate <= config.getRisePercentage()) {
        log.debug("设备{}CPM上升率{}%未超过阈值{}%",
                  deviceCode, riseRate * 100, config.getRisePercentage() * 100);
        return;
    }

    // 7. 检查告警去重（最小间隔）
    LocalDateTime lastAlertTime = getLastCpmRiseAlertTime(deviceCode);
    if (lastAlertTime != null) {
        long minutesSinceLastAlert = ChronoUnit.MINUTES.between(
            lastAlertTime, LocalDateTime.now()
        );
        if (minutesSinceLastAlert < config.getMinInterval() / 60) {
            log.debug("设备{}距离上次告警仅{}分钟，未超过最小间隔{}分钟",
                      deviceCode, minutesSinceLastAlert, config.getMinInterval() / 60);
            return;
        }
    }

    // 8. 触发告警
    String message = String.format(
        "辐射值突增: 从%.2f CPM上升至%.2f CPM（上升%.1f%%），超过阈值%.0f%%",
        lastCpm, currentCpm, riseRate * 100, config.getRisePercentage() * 100
    );

    createAlert(
        AlertType.CPM_RISE,
        AlertSeverity.CRITICAL,
        deviceCode,
        deviceId,
        companyId,
        message,
        Map.of(
            "lastCpm", lastCpm,
            "currentCpm", currentCpm,
            "riseRate", riseRate,
            "threshold", config.getRisePercentage()
        )
    );

    // 9. 更新告警去重缓存
    updateLastCpmRiseAlertTime(deviceCode, LocalDateTime.now());

    log.warn("⚠️ CPM上升率告警触发: deviceCode={}, riseRate={}%",
             deviceCode, riseRate * 100);
}
```

### 依赖的新方法

#### DeviceStatusCacheService扩展
```java
// 在DeviceStatusCacheService中添加：

/**
 * 获取设备上次CPM上升率告警时间
 */
public LocalDateTime getLastCpmRiseAlertTime(String deviceCode) {
    String key = buildCacheKey(deviceCode);
    String value = getStringValue(key, "lastCpmRiseAlertAt");
    return value != null ? LocalDateTime.parse(value, ISO_FORMATTER) : null;
}

/**
 * 更新设备上次CPM上升率告警时间
 */
public void updateLastCpmRiseAlertTime(String deviceCode, LocalDateTime alertTime) {
    String key = buildCacheKey(deviceCode);
    redisTemplate.opsForHash().put(key, "lastCpmRiseAlertAt",
        alertTime.format(ISO_FORMATTER));
    redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
}
```

---

## 2️⃣ 设备离线定时检查任务

### 业务需求

**背景**：设备本身不发送上线/下线/故障信息，需要根据是否有数据消息来判断在线/离线状态。

**关键点**：
- 定时扫描所有设备
- 检查最后消息时间（lastMessageAt）
- 超时未收到消息 → 设备离线
- 触发OFFLINE告警
- 避免重复告警（去重）

### 算法设计

#### 核心逻辑
```
离线判断 = (当前时间 - 最后消息时间) > 离线超时时间

默认配置：10分钟无数据 → 视为离线
```

#### 实现流程

```
定时任务触发（每1分钟）
    ↓
获取所有激活的设备
    ↓
遍历每个设备
    ↓
从Redis获取lastMessageAt
    ↓
判断是否超时
    ├─ lastMessageAt为null → 设备从未上线，跳过
    ├─ 距离现在 < timeoutMinutes → 在线，跳过
    └─ 距离现在 > timeoutMinutes → 离线
        ↓
    检查是否已有未解决的OFFLINE告警
        ├─ 已存在 → 不重复告警
        └─ 不存在 → 触发OFFLINE告警
            ↓
        更新设备状态为OFFLINE（缓存和数据库）
```

### 定时任务配置

```java
@Scheduled(
    initialDelay = 60000,    // 启动后1分钟开始
    fixedRate = 60000        // 每1分钟执行一次
)
public void checkDeviceOffline() {
    // 实现逻辑
}
```

**为什么1分钟检查一次？**
- 配置的离线超时是10分钟
- 1分钟检查频率可以快速发现离线
- 不会对性能造成压力

### 边界条件处理

| 场景 | 处理方式 |
|------|----------|
| 设备从未上线（lastMessageAt为null） | 跳过检查，不触发离线告警 |
| 设备刚激活，还未收到第一条数据 | 跳过检查 |
| 设备已删除或停用 | 不检查，过滤掉 |
| 已有未解决的OFFLINE告警 | 不重复告警 |
| 设备重新上线（收到新数据） | 自动解决旧告警或标记已解决 |
| Redis缓存丢失 | 从数据库查询lastOnlineAt作为降级 |

### 告警自动解决逻辑

**方案A**：设备重新上线时自动解决旧告警
```java
// 在MqttMessageListener中，收到消息时：
if (deviceWasOffline) {
    // 解决所有未解决的OFFLINE告警
    resolveOfflineAlerts(deviceCode);
}
```

**方案B**：定时任务发现设备在线时解决
```java
// 在离线检查任务中：
if (deviceIsOnline && hasUnresolvedOfflineAlert) {
    resolveOfflineAlert(deviceCode);
}
```

**推荐**：方案A，在MQTT消息到达时即时解决，用户体验更好。

### 实现伪代码

```java
@Scheduled(initialDelay = 60000, fixedRate = 60000)
public void checkDeviceOffline() {
    log.debug("开始检查设备离线状态...");

    // 1. 获取配置
    var config = alertConfigService.getOfflineTimeoutConfig();
    int timeoutMinutes = config.getTimeoutMinutes();
    LocalDateTime offlineThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

    // 2. 获取所有激活的设备
    List<Device> activeDevices = deviceRepository.findByActivationStatus(
        DeviceActivationStatus.ACTIVE
    );

    int offlineCount = 0;

    // 3. 遍历检查每个设备
    for (Device device : activeDevices) {
        try {
            // 从缓存获取最后消息时间
            LocalDateTime lastMessageTime = cacheService.getLastMessageTime(
                device.getDeviceCode()
            );

            // 首次启动或缓存丢失，从数据库查询
            if (lastMessageTime == null) {
                lastMessageTime = device.getLastOnlineAt();
            }

            // 设备从未上线，跳过
            if (lastMessageTime == null) {
                continue;
            }

            // 判断是否离线
            if (lastMessageTime.isBefore(offlineThreshold)) {
                // 设备离线，检查是否已有告警
                boolean hasExistingAlert = alertRepository
                    .findByDeviceIdAndAlertTypeAndResolved(
                        device.getId(),
                        AlertType.OFFLINE.getCode(),
                        false
                    ).isPresent();

                if (!hasExistingAlert) {
                    // 触发离线告警
                    String offlineDuration = formatDuration(
                        Duration.between(lastMessageTime, LocalDateTime.now())
                    );

                    createAlert(
                        AlertType.OFFLINE,
                        AlertSeverity.WARNING,
                        device.getDeviceCode(),
                        device.getId(),
                        device.getCompany().getId(),
                        String.format(
                            "设备离线: 最后消息时间为%s，已离线%s",
                            lastMessageTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            offlineDuration
                        ),
                        Map.of(
                            "lastMessageAt", lastMessageTime.toString(),
                            "offlineMinutes", ChronoUnit.MINUTES.between(
                                lastMessageTime, LocalDateTime.now()
                            )
                        )
                    );

                    // 更新设备状态
                    cacheService.updateStatus(device.getDeviceCode(), "OFFLINE");

                    offlineCount++;
                }
            } else {
                // 设备在线，检查是否需要解决旧告警
                resolveOfflineAlertIfNeeded(device);
            }

        } catch (Exception e) {
            log.error("检查设备{}离线状态失败", device.getDeviceCode(), e);
        }
    }

    if (offlineCount > 0) {
        log.warn("⚠️ 发现{}个设备离线", offlineCount);
    }
}

private void resolveOfflineAlertIfNeeded(Device device) {
    // 查找未解决的OFFLINE告警
    List<Alert> unresolvedAlerts = alertRepository
        .findByDeviceIdAndAlertTypeAndResolved(
            device.getId(),
            AlertType.OFFLINE.getCode(),
            false
        );

    if (!unresolvedAlerts.isEmpty()) {
        // 解决所有离线告警
        unresolvedAlerts.forEach(alert -> {
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
        });

        log.info("✅ 设备{}重新上线，解决{}个离线告警",
                 device.getDeviceCode(), unresolvedAlerts.size());
    }
}
```

### 配置参数

```yaml
app:
  ems:
    alert:
      offline-timeout:
        timeout-minutes: 10      # 10分钟无数据视为离线
```

---

## 3️⃣ 低电压告警配置化

### 业务需求

**当前状态**：使用硬编码阈值 `LOW_BATTERY_THRESHOLD = 3.5`

**目标**：从 `AlertConfigService` 读取配置，支持动态调整。

### 实现方案

#### 修改点

**AlertService.java**
```java
// 旧代码：
private static final double LOW_BATTERY_THRESHOLD = 3.5;

if (battery != null && battery < LOW_BATTERY_THRESHOLD) {
    // ... 触发告警
}

// 新代码：
private final AlertConfigService alertConfigService;

public void checkEnvironmentDataAndAlert(String deviceCode, Double battery,
                                         Long deviceId, Long companyId) {
    // 从配置服务读取阈值
    var config = alertConfigService.getLowBatteryConfig();
    double voltageThreshold = config.getVoltageThreshold();

    if (battery != null && battery < voltageThreshold) {
        createAlert(
            AlertType.LOW_BATTERY,
            AlertSeverity.WARNING,
            deviceCode,
            deviceId,
            companyId,
            String.format(
                "电量不足: 当前电压%.2f V，低于阈值%.1f V",
                battery, voltageThreshold
            ),
            Map.of(
                "battery", battery,
                "threshold", voltageThreshold
            )
        );
    }
}
```

#### 需要添加的依赖

```java
@RequiredArgsConstructor
public class AlertService {
    // ... 其他依赖

    private final AlertConfigService alertConfigService;  // 新增

    // 移除硬编码常量
    // private static final double LOW_BATTERY_THRESHOLD = 3.5;  // 删除
}
```

### 边界条件处理

| 场景 | 处理方式 |
|------|----------|
| battery为null | 不触发告警 |
| battery正好等于阈值 | 不触发告警（< 而非 <=） |
| 环境设备和辐射设备电池单位不同 | 辐射设备已转换为V，环境设备直接使用 |

### 配置参数

```yaml
app:
  ems:
    alert:
      low-battery:
        voltage-threshold: 3.5    # 3.5V以下触发告警
```

### 环境数据检查

**注意**：环境设备数据中的battery字段已经是伏特单位，不需要转换。
**辐射设备**：Batvolt字段需要除以1000转换为伏特（已在MQTT监听器中处理）。

---

## 📊 实现优先级

| 功能 | 优先级 | 预计工作量 | 依赖 |
|------|--------|-----------|------|
| 低电压告警配置化 | 低 | 30分钟 | AlertConfigService |
| CPM上升率检查 | 高 | 2小时 | 缓存服务、配置服务 |
| 设备离线检查 | 高 | 2小时 | 缓存服务、告警仓库 |

**推荐实现顺序**：
1. 低电压告警配置化（最简单，热身）
2. CPM上升率检查（核心业务逻辑）
3. 设备离线检查（定时任务）

---

## 🗂️ 需要新增的文件

### 1. DeviceOfflineCheckScheduler.java
```
路径: backend/src/main/java/com/cdutetc/ems/scheduler/
作用: 设备离线定时检查任务
```

### 2. AlertRepository扩展方法
```
路径: backend/src/main/java/com/cdutetc/ems/repository/AlertRepository.java
新增方法:
  - findByDeviceIdAndAlertTypeAndResolved()
  - findByDeviceIdAndResolved()
```

### 3. DeviceRepository扩展方法
```
路径: backend/src/main/java/com/cdutetc/ems/repository/DeviceRepository.java
新增方法:
  - findByActivationStatus()
```

### 4. DeviceStatusCacheService扩展方法
```
路径: backend/src/main/java/com/cdutetc/ems/service/DeviceStatusCacheService.java
新增方法:
  - getLastCpmRiseAlertTime()
  - updateLastCpmRiseAlertTime()
```

---

## ✅ 验证方案

### 单元测试

1. **CPM上升率测试**
   - 正常上升率告警
   - 基数太小不告警
   - 告警去重测试
   - CPM下降不告警

2. **设备离线测试**
   - 超时触发告警
   - 重复告警防护
   - 重新上线自动解决

3. **低电压测试**
   - 低于阈值告警
   - 等于阈值不告警
   - 配置动态调整

### 集成测试

1. 端到端MQTT消息测试
2. Redis缓存一致性测试
3. 定时任务执行测试

---

**设计完成时间**：2025-12-27
**下一步**：进入细化方案阶段
