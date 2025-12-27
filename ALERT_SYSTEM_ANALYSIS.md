# 告警系统实现分析报告

## 执行时间
2025-12-27

---

## 📊 **系统架构概览**

### 整体流程图

```
[设备数据采集] → [MQTT/REST] → [MqttMessageListener] 
                                    ↓
                            [AlertService检查]
                                    ↓
                    ┌───────────────┴───────────────┐
                    ↓                               ↓
              [触发告警]                      [通过SSE推送]
                    ↓                               ↓
            [存入数据库]                    [前端实时通知]
                    ↓                               ↓
            [告警列表] ←──── [用户查看/处理] ←──── [AlertNotification]
```

---

## 🏗️ **后端实现分析**

### 1. 数据模型（Alert实体）

**文件**：`backend/src/main/java/com/cdutetc/ems/entity/Alert.java`

#### 核心字段
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| alertType | String | 告警类型（HIGH_CPM, OFFLINE, FAULT, LOW_BATTERY） |
| severity | String | 严重程度（CRITICAL, WARNING, INFO） |
| deviceCode | String | 设备编码 |
| device | Device | 关联设备（@ManyToOne） |
| company | Company | 关联企业（@ManyToOne） |
| message | String | 告警消息（TEXT） |
| data | String | 详细数据（JSON格式） |
| resolved | Boolean | 是否已解决 |
| resolvedAt | LocalDateTime | 解决时间 |
| createdAt | LocalDateTime | 创建时间 |

#### 设计特点
- ✅ 支持多租户（通过company关联）
- ✅ 关联设备和设备编码（冗余设计，便于查询）
- ✅ JSON字段存储详细数据（灵活扩展）
- ✅ 软删除机制（resolved字段）

---

### 2. 告警类型和严重程度

#### AlertType枚举
**文件**：`backend/src/main/java/com/cdutetc/ems/entity/enums/AlertType.java`

| 枚举值 | Code | 描述 | 触发条件 |
|--------|------|------|----------|
| HIGH_CPM | HIGH_CPM | 辐射值超标 | CPM > 100 |
| OFFLINE | OFFLINE | 设备离线 | 离线超过10分钟 |
| FAULT | FAULT | 设备故障 | 设备状态=FAULT |
| LOW_BATTERY | LOW_BATTERY | 电量不足 | 电池电压 < 3.5V |

#### AlertSeverity枚举
**文件**：`backend/src/main/java/com/cdutetc/ems/entity/enums/AlertSeverity.java`

| 枚举值 | Code | 描述 | 处理优先级 |
|--------|------|------|-----------|
| CRITICAL | CRITICAL | 严重 | 🔴 立即处理 |
| WARNING | WARNING | 警告 | 🟠 尽快处理 |
| INFO | INFO | 信息 | 🔵 关注即可 |

---

### 3. AlertService核心功能

**文件**：`backend/src/main/java/com/cdutetc/ems/service/AlertService.java`

#### 3.1 告警触发机制

##### **辐射数据告警检查** (117-130行)
```java
public void checkRadiationDataAndAlert(String deviceCode, Double cpm, Long deviceId, Long companyId) {
    if (cpm != null && cpm > HIGH_CPM_THRESHOLD) {  // 阈值：100 CPM
        createAlert(
            AlertType.HIGH_CPM,
            AlertSeverity.CRITICAL,
            deviceCode,
            deviceId,
            companyId,
            String.format("辐射值超标: 当前值 %.2f CPM，阈值 %d CPM", cpm, 100),
            Map.of("cpm", cpm, "threshold", 100.0)
        );
    }
}
```

**调用时机**：MQTT接收到辐射设备数据后（MqttMessageListener:230）

##### **环境数据告警检查** (135-148行)
```java
public void checkEnvironmentDataAndAlert(String deviceCode, Double battery, Long deviceId, Long companyId) {
    if (battery != null && battery < LOW_BATTERY_THRESHOLD) {  // 阈值：3.5V
        createAlert(
            AlertType.LOW_BATTERY,
            AlertSeverity.WARNING,
            deviceCode,
            deviceId,
            companyId,
            String.format("电量不足: 当前电压 %.2f V，阈值 %.1f V", battery, 3.5),
            Map.of("battery", battery, "threshold", 3.5)
        );
    }
}
```

**调用时机**：MQTT接收到环境设备数据后（MqttMessageListener:305）

##### **设备状态告警检查** (153-192行)
```java
public void checkDeviceStatusAndAlert(Device device) {
    // 1. 设备故障告警
    if (DeviceStatus.FAULT.name().equals(device.getStatus())) {
        createAlert(...);  // CRITICAL级别
    }
    
    // 2. 设备离线告警（离线超过10分钟）
    if (DeviceStatus.OFFLINE.name().equals(device.getStatus())) {
        LocalDateTime offlineThreshold = LocalDateTime.now().minusMinutes(10);
        if (device.getLastOnlineAt().isBefore(offlineThreshold)) {
            // 防止重复告警：检查是否已有未解决的离线告警
            boolean hasOfflineAlert = existingAlerts.stream()
                .anyMatch(a -> a.getAlertType().equals(AlertType.OFFLINE.getCode())
                        && !a.getResolved());
            
            if (!hasOfflineAlert) {
                createAlert(...);  // WARNING级别
            }
        }
    }
}
```

**⚠️ 注意**：此方法**未在代码中被调用**，需要补充调用逻辑！

---

#### 3.2 告警创建流程

```java
createAlert() → 存储数据库 → 通过SSE推送 → 前端实时通知
```

**关键代码** (46-84行)：
1. 创建Alert对象
2. 关联Device和Company
3. 序列化data字段为JSON
4. 保存到数据库
5. **通过SSE推送到前端** (89-112行)

---

#### 3.3 SSE实时推送

**实现方式**：Spring SseEmitter

```java
private void pushAlertViaSSE(Alert alert) {
    DeviceDataEvent event = new DeviceDataEvent(
        "alert",                          // 事件类型
        alert.getDeviceCode(),
        "ALERT",
        Map.of(                          // 推送数据
            "alertId", alert.getId(),
            "alertType", alert.getAlertType(),
            "severity", alert.getSeverity(),
            "deviceCode", alert.getDeviceCode(),
            "message", alert.getMessage(),
            "timestamp", alert.getCreatedAt().toString()
        )
    );
    
    // 广播到企业下的所有在线用户
    sseEmitterService.broadcastDeviceData(alert.getCompany().getId(), event);
}
```

---

#### 3.4 告警查询接口

| 方法 | 功能 | 返回类型 |
|------|------|----------|
| getAlerts() | 分页查询告警列表 | Page<Alert> |
| getUnresolvedAlerts() | 查询未解决告警 | List<Alert> |
| getRecentAlerts() | 查询最近告警 | List<Alert> |
| getAlertsByType() | 按类型查询 | List<Alert> |
| countUnresolvedAlerts() | 统计未解决数量 | long |
| getAlertStatistics() | 统计告警（按严重程度） | Map<String, Long> |

---

#### 3.5 告警处理

```java
// 单个告警解决
resolveAlert(Long alertId, Long companyId)

// 批量解决设备告警
resolveAlertsByDevice(Long deviceId, Long companyId)
```

---

### 4. AlertController API接口

**文件**：`backend/src/main/java/com/cdutetc/ems/controller/AlertController.java`

#### REST API列表

| 端点 | 方法 | 功能 | 权限 |
|------|------|------|------|
| /alerts | GET | 获取告警列表（分页） | 登录用户 |
| /alerts/unresolved | GET | 获取未解决告警 | 登录用户 |
| /alerts/recent | GET | 获取最近告警 | 登录用户 |
| /alerts/type/{alertType} | GET | 按类型查询告警 | 登录用户 |
| /alerts/statistics | GET | 获取告警统计 | 登录用户 |
| /alerts/{id}/resolve | POST | 解决告警 | 登录用户 |
| /alerts/device/{deviceId}/resolve-all | POST | 批量解决设备告警 | 登录用户 |

#### 权限设计
- 基于企业隔离（通过getCurrentUser().getCompany()）
- 用户只能查看和操作自己企业的告警

---

## 💻 **前端实现分析**

### 1. SSE连接管理

**文件**：`frontend/src/utils/sse.js`

#### 核心类：DeviceSSE
```javascript
class DeviceSSE {
  constructor(messageCallback) {
    this.eventSource = null
    this.messageCallback = messageCallback
  }
  
  connect() {
    // 创建EventSource连接
    // 通过URL参数传递token
    this.eventSource = new EventSource('/api/sse/subscribe?token=' + token)
    
    // 监听事件类型
    this.eventSource.addEventListener('radiation-data', ...)
    this.eventSource.addEventListener('environment-data', ...)
    this.eventSource.addEventListener('alert', ...)  // 告警事件
  }
}
```

#### 单例管理：SSEManager
- 全局唯一SSE连接
- 支持多订阅者模式
- 自动重连机制

---

### 2. 告警通知组件

**文件**：`frontend/src/components/AlertNotification.vue`

#### 功能特性
```javascript
// 订阅告警事件
unsubscribeAlert = sseManager.subscribe('alert', handleAlertMessage)

// 根据严重程度显示不同类型的通知
if (severity === 'HIGH') {
  type = 'error'      // 红色通知，需手动关闭
  title = '🚨 高危告警'
} else if (severity === 'MEDIUM') {
  type = 'warning'    // 橙色通知
  title = '⚠️ 中等告警'
} else {
  type = 'info'       // 蓝色通知，5秒自动关闭
  title = 'ℹ️ 低危告警'
}
```

#### 交互设计
- 点击通知跳转到告警列表页
- 支持手动关闭高危告警
- 视觉增强（左侧彩色边框）

---

### 3. 告警列表页面

**文件**：`frontend/src/views/alerts/AlertList.vue`

#### 功能模块
1. **统计卡片**：显示未解决/高/中/低危告警数量
2. **筛选表单**：按解决状态筛选
3. **告警表格**：分页显示告警列表
4. **操作按钮**：解决告警

#### 表格列
- 设备编码
- 设备名称
- 告警类型
- 严重程度（带颜色标签）
- 告警消息
- 发生时间
- 状态（已解决/未解决）
- 操作（解决按钮）

---

## ⚠️ **存在的问题和建议**

### 问题1：设备状态告警未触发

**现象**：
```java
// AlertService中定义了checkDeviceStatusAndAlert()方法
// 但在代码库中没有任何调用
```

**影响**：
- ❌ 设备离线告警不会触发
- ❌ 设备故障告警不会触发

**建议修复**：
在设备状态更新时调用告警检查，例如：
```java
// DeviceService或定时任务中
public void updateDeviceStatus(Device device, DeviceStatus newStatus) {
    device.setStatus(newStatus);
    deviceRepository.save(device);
    
    // 触发告警检查
    alertService.checkDeviceStatusAndAlert(device);
}
```

---

### 问题2：告警阈值硬编码

**现象**：
```java
private static final double HIGH_CPM_THRESHOLD = 100.0;
private static final double LOW_BATTERY_THRESHOLD = 3.5;
```

**问题**：
- ❌ 不同企业可能需要不同的阈值
- ❌ 不同设备类型可能需要不同的阈值
- ❌ 修改阈值需要重新编译代码

**建议优化**：
```java
// 方案1：数据库配置
@Entity
public class AlertThreshold {
    private Long companyId;
    private Long deviceTypeId;
    private String alertType;
    private Double threshold;
    private Double criticalValue;
}

// 方案2：配置文件
@ConfigurationProperties(prefix = "alert.threshold")
public class AlertThresholdConfig {
    private Map<String, Double> radiation;
    private Map<String, Double> battery;
}
```

---

### 问题3：缺少告警升级机制

**现象**：
- 高危告警一直未处理，没有升级通知

**建议**：
```java
// 定时任务：检查长时间未解决的高危告警
@Scheduled(fixedRate = 300000) // 每5分钟
public void escalateAlerts() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
    List<Alert> oldAlerts = alertRepository.findUnresolvedAlertsBefore(threshold);
    
    for (Alert alert : oldAlerts) {
        // 发送升级通知（邮件、短信、钉钉等）
        notificationService.sendEscalation(alert);
    }
}
```

---

### 问题4：缺少告警去重机制

**现象**：
```java
// 辐射值告警每次MQTT消息都会触发
// 如果设备持续超标，会产生大量重复告警
```

**建议**：
```java
// 方案1：基于时间窗口去重
public void checkRadiationDataAndAlert(...) {
    if (cpm > threshold) {
        // 检查最近N分钟是否已有相同类型告警
        LocalDateTime window = LocalDateTime.now().minusMinutes(10);
        boolean hasRecentAlert = alertRepository.existsByDeviceAndTypeAndTime(
            deviceCode, AlertType.HIGH_CPM, window
        );
        
        if (!hasRecentAlert) {
            createAlert(...);
        }
    }
}

// 方案2：告警聚合
public class AggregatedAlert {
    private String deviceCode;
    private AlertType alertType;
    private int count;           // 触发次数
    private LocalDateTime firstOccurrence;
    private LocalDateTime lastOccurrence;
}
```

---

### 问题5：SSE连接断线重连机制不完善

**现象**：
```javascript
// SSE断开后需要用户手动刷新页面
// 自动重连逻辑存在，但未实现指数退避
```

**建议**：
```javascript
// 指数退避重连
class DeviceSSE {
  reconnect(attempt = 0) {
    const delay = Math.min(1000 * Math.pow(2, attempt), 30000); // 最大30秒
    setTimeout(() => {
      this.connect();
    }, delay);
  }
}
```

---

## 📈 **优化建议总结**

### 高优先级（影响功能）
1. ✅ **补充设备状态告警调用** - 关键功能缺失
2. ✅ **实现告警去重机制** - 防止告警风暴
3. ✅ **优化阈值配置** - 支持动态配置

### 中优先级（提升体验）
4. ✅ **实现告警升级机制** - 确保高危告警被处理
5. ✅ **完善SSE重连机制** - 提升稳定性
6. ✅ **添加告警历史趋势分析** - 数据可视化

### 低优先级（长期规划）
7. 🔵 支持多渠道通知（邮件、短信、钉钉、企业微信）
8. 🔵 实现告警规则引擎（支持自定义规则）
9. 🔵 添加告警预测功能（基于历史数据）

---

## ✅ **现有优势**

1. **实时性好**
   - SSE推送机制，延迟低
   - 告警即时通知

2. **架构清晰**
   - 前后端分离
   - 多租户隔离
   - 职责明确

3. **可扩展性强**
   - JSON字段存储详细数据
   - 易于添加新的告警类型

4. **用户体验好**
   - 前端通知样式丰富
   - 支持点击跳转
   - 告警统计清晰

---

*报告生成时间：2025-12-27*
*分析人：Claude Code*
