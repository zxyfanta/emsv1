# 告警系统重构设计方案

## 业务需求澄清

### 核心业务逻辑

#### 1. **CPM（辐射值）告警** - 基于上升率

**业务背景**：
- CPM是累加值，会随着时间缓慢上升
- 不能使用绝对值阈值判断（如 > 100）
- 应该检查**单次上升幅度**

**正确逻辑**：
```
上次CPM值: 100
当前CPM值: 120
上升幅度: 120 - 100 = 20
上升率: 20 / 100 = 20%
配置阈值: 15%

if (上升率 > 15%) {
    触发CPM突增告警
}
```

**配置参数**：
- `risePercentage`: 上升率阈值（如 0.15 = 15%）
- `minInterval`: 最小检查间隔（秒），防止频繁告警
- `minCpm`: 最小CPM基数（避免基数太小导致误报）

---

#### 2. **设备在线/离线判断** - 基于数据消息

**业务背景**：
- 设备**不发送**上线/下线/故障状态消息
- 通过**是否有数据消息**判断在线状态

**判断逻辑**：
```
收到MQTT数据消息：
  → 更新 device.lastMessageAt = now()
  → 设备标记为在线

定时任务（每分钟）：
  → 检查所有设备
  → if (now() - device.lastMessageAt > 配置超时时间) {
      设备离线，触发告警
    }
```

**配置参数**：
- `timeoutMinutes`: 超时时间（分钟）

---

#### 3. **低电压告警** - 电压阈值

**逻辑**：
```
if (battery < 配置电压阈值) {
    触发低电压告警
}
```

**配置参数**：
- `voltageThreshold`: 电压阈值（V，如 3.5V）

---

### 告警类型重新定义

| 告警类型 | 触发条件 | 配置参数 | 移除 |
|---------|----------|----------|------|
| CPM突增告警 | 单次上升率 > 阈值 | risePercentage | HIGH_CPM |
| 低电压告警 | 电池电压 < 阈值 | voltageThreshold | - |
| 设备离线告警 | 超时无数据 | timeoutMinutes | - |
| ~~设备故障告警~~ | ~~设备状态=FAULT~~ | - | ❌ 移除 |

---

## 技术方案设计

### 方案架构

```
┌─────────────────────────────────────────────────────────┐
│                    告警配置层                           │
│  AlertConfig (企业级别配置 + 全局默认配置)               │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    告警检查层                           │
│  AlertService.checkRadiationDataAndAlert()             │
│  AlertService.checkEnvironmentDataAndAlert()           │
│  OfflineCheckScheduler (定时任务)                       │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    告警触发层                           │
│  创建Alert → 保存到数据库 → SSE推送 → 前端通知          │
└─────────────────────────────────────────────────────────┘
```

---

## 实现步骤

### 步骤1：数据库模型修改

#### 1.1 AlertConfig配置实体 ✅

**文件**：`backend/src/main/java/com/cdutetc/ems/entity/AlertConfig.java`

**字段设计**：
```java
@Entity
public class AlertConfig {
    private Long id;
    private Company company;           // null表示全局默认配置
    private String configType;        // CPM_RISE, LOW_BATTERY, OFFLINE_TIMEOUT
    private String configValue;       // JSON格式的配置对象
    private Boolean enabled;
    
    // 内部配置类
    static class CpmRiseConfig {
        double risePercentage;  // 0.15 (15%)
        int minInterval;        // 300 (5分钟)
        int minCpm;             // 50
    }
    
    static class LowBatteryConfig {
        double voltageThreshold;  // 3.5V
    }
    
    static class OfflineTimeoutConfig {
        int timeoutMinutes;  // 10 (10分钟)
    }
}
```

**默认配置数据**：
```sql
-- 全局默认配置
INSERT INTO alert_configs (config_type, config_value, enabled) VALUES 
('CPM_RISE', '{"risePercentage":0.15,"minInterval":300,"minCpm":50}', true),
('LOW_BATTERY', '{"voltageThreshold":3.5}', true),
('OFFLINE_TIMEOUT', '{"timeoutMinutes":10}', true);
```

#### 1.2 Device实体增强 ✅

**新增字段**：
```java
@Column(name = "last_message_at")
private LocalDateTime lastMessageAt;  // 最后一次收到MQTT数据的时间
```

**使用场景**：
```java
// MqttMessageListener中
device.setLastMessageAt(LocalDateTime.now());
deviceRepository.save(device);

// 定时任务中
LocalDateTime now = LocalDateTime.now();
LocalDateTime threshold = now.minusMinutes(timeoutMinutes);
if (device.getLastMessageAt().isBefore(threshold)) {
    // 设备离线
}
```

---

### 步骤2：告警逻辑重构

#### 2.1 CPM突增告警

**实现逻辑**：
```java
public void checkRadiationDataAndAlert(String deviceCode, Double currentCpm, 
                                       Long deviceId, Long companyId) {
    // 1. 获取配置
    CpmRiseConfig config = getAlertConfig(companyId, "CPM_RISE", CpmRiseConfig.class);
    
    // 2. 检查最小CPM基数
    if (currentCpm < config.getMinCpm()) {
        return;  // CPM太小，不检查
    }
    
    // 3. 获取上次的CPM值（从最近的数据记录）
    Double lastCpm = getLastCpmValue(deviceCode);
    
    if (lastCpm != null && lastCpm > 0) {
        // 4. 计算上升率
        double rise = currentCpm - lastCpm;
        double riseRate = rise / lastCpm;
        
        // 5. 检查是否超过阈值
        if (riseRate > config.getRisePercentage()) {
            // 6. 检查冷却时间（防止频繁告警）
            if (canCreateAlert(deviceCode, AlertType.CPM_RISE, config.getMinInterval())) {
                createAlert(
                    AlertType.CPM_RISE,
                    AlertSeverity.WARNING,
                    deviceCode,
                    deviceId,
                    companyId,
                    String.format("辐射值突增告警: 上次值=%.2f, 当前值=%.2f, 上升率=%.2f%%", 
                               lastCpm, currentCpm, riseRate * 100),
                    Map.of(
                        "lastCpm", lastCpm,
                        "currentCpm", currentCpm,
                        "riseRate", riseRate,
                        "threshold", config.getRisePercentage()
                    )
                );
            }
        }
    }
}
```

**Repository方法**：
```java
@Query("SELECT r.cpm FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode ORDER BY r.recordTime DESC")
Optional<Double> findLastCpmByDeviceCode(@Param("deviceCode") String deviceCode);
```

---

#### 2.2 低电压告警（已实现，需改为使用配置）

```java
public void checkEnvironmentDataAndAlert(String deviceCode, Double battery, 
                                         Long deviceId, Long companyId) {
    // 获取配置
    LowBatteryConfig config = getAlertConfig(companyId, "LOW_BATTERY", LowBatteryConfig.class);
    
    if (battery < config.getVoltageThreshold()) {
        createAlert(
            AlertType.LOW_BATTERY,
            AlertSeverity.WARNING,
            deviceCode,
            deviceId,
            companyId,
            String.format("电量不足: 当前电压=%.2fV, 阈值=%.2fV", 
                       battery, config.getVoltageThreshold()),
            Map.of("battery", battery, "threshold", config.getVoltageThreshold())
        );
    }
}
```

---

#### 2.3 设备离线告警（定时任务）

**定时任务类**：
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceOfflineCheckScheduler {
    
    private final DeviceRepository deviceRepository;
    private final AlertService alertService;
    private final AlertConfigService alertConfigService;
    
    /**
     * 每分钟检查一次设备在线状态
     */
    @Scheduled(fixedRate = 60000)  // 60秒
    public void checkDeviceOnlineStatus() {
        try {
            // 1. 获取所有已激活的设备
            List<Device> devices = deviceRepository.findByActivationStatus(
                DeviceActivationStatus.ACTIVE
            );
            
            // 2. 按企业分组处理
            Map<Long, List<Device>> devicesByCompany = devices.stream()
                .collect(Collectors.groupingBy(d -> d.getCompany().getId()));
            
            // 3. 检查每个企业的设备
            for (Map.Entry<Long, List<Device>> entry : devicesByCompany.entrySet()) {
                Long companyId = entry.getKey();
                List<Device> companyDevices = entry.getValue();
                
                // 获取该企业的离线超时配置
                OfflineTimeoutConfig config = alertConfigService.getConfig(
                    companyId, 
                    "OFFLINE_TIMEOUT", 
                    OfflineTimeoutConfig.class
                );
                
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime threshold = now.minusMinutes(config.getTimeoutMinutes());
                
                for (Device device : companyDevices) {
                    // 检查最后消息时间
                    if (device.getLastMessageAt() == null) {
                        // 从未收到过消息，跳过
                        continue;
                    }
                    
                    if (device.getLastMessageAt().isBefore(threshold)) {
                        // 设备离线，检查是否需要创建告警
                        boolean hasUnresolvedOfflineAlert = checkExistingOfflineAlert(device);
                        
                        if (!hasUnresolvedOfflineAlert) {
                            alertService.createAlert(
                                AlertType.OFFLINE,
                                AlertSeverity.WARNING,
                                device.getDeviceCode(),
                                device.getId(),
                                companyId,
                                String.format("设备离线: 最后消息时间=%s", 
                                           device.getLastMessageAt()),
                                Map.of(
                                    "lastMessageAt", device.getLastMessageAt().toString(),
                                    "timeoutMinutes", config.getTimeoutMinutes()
                                )
                            );
                            
                            log.info("设备离线告警已创建: {}", device.getDeviceCode());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查设备在线状态失败", e);
        }
    }
    
    private boolean checkExistingOfflineAlert(Device device) {
        // 检查是否已有未解决的离线告警
        return alertRepository.existsByDeviceAndTypeAndResolved(
            device.getId(), 
            AlertType.OFFLINE.getCode(), 
            false
        );
    }
}
```

**启用定时任务**：
```java
@SpringBootApplication
@EnableScheduling  // 添加这个注解
public class EmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmsApplication.class, args);
    }
}
```

---

### 步骤3：MqttMessageListener修改

#### 3.1 更新设备最后消息时间

```java
private void handleRadiationData(Device device, String payload) {
    try {
        // ... 解析数据 ...
        
        // 更新最后消息时间 ⭐ 新增
        device.setLastMessageAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
        
        // 检查告警（使用新的逻辑）
        alertService.checkRadiationDataAndAlert(
            device.getDeviceCode(),
            savedData.getCpm(),
            device.getId(),
            device.getCompany().getId()
        );
        
    } catch (Exception e) {
        log.error("❌ 处理辐射设备数据失败: {}", device.getDeviceCode(), e);
    }
}

private void handleEnvironmentData(Device device, String payload) {
    try {
        // ... 解析数据 ...
        
        // 更新最后消息时间 ⭐ 新增
        device.setLastMessageAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
        
        // 检查告警（使用配置）
        alertService.checkEnvironmentDataAndAlert(
            device.getDeviceCode(),
            savedData.getBattery(),
            device.getId(),
            device.getCompany().getId()
        );
        
    } catch (Exception e) {
        log.error("❌ 处理环境设备数据失败: {}", device.getDeviceCode(), e);
    }
}
```

---

### 步骤4：配置管理服务

```java
@Service
@RequiredArgsConstructor
public class AlertConfigService {
    
    private final AlertConfigRepository alertConfigRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 获取告警配置
     * 优先使用企业级配置，如果不存在则使用全局默认配置
     */
    public <T> T getConfig(Long companyId, String configType, Class<T> configClass) {
        // 1. 尝试获取企业级配置
        AlertConfig config = alertConfigRepository
            .findByCompanyIdAndConfigType(companyId, configType)
            .orElse(null);
        
        // 2. 如果不存在，获取全局默认配置
        if (config == null) {
            config = alertConfigRepository
                .findByCompanyIdIsNullAndConfigType(configType)
                .orElseThrow(() -> new IllegalStateException("告警配置不存在: " + configType));
        }
        
        // 3. 解析JSON配置
        try {
            return objectMapper.readValue(config.getConfigValue(), configClass);
        } catch (Exception e) {
            throw new RuntimeException("解析告警配置失败", e);
        }
    }
    
    /**
     * 更新企业级配置
     */
    public void updateConfig(Long companyId, String configType, Object config) {
        AlertConfig alertConfig = alertConfigRepository
            .findByCompanyIdAndConfigType(companyId, configType)
            .orElse(new AlertConfig());
        
        alertConfig.setCompanyId(companyId);
        alertConfig.setConfigType(configType);
        
        try {
            alertConfig.setConfigValue(objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            throw new RuntimeException("序列化配置失败", e);
        }
        
        alertConfigRepository.save(alertConfig);
    }
}
```

---

## 配置界面建议

### 前端配置页面

```vue
<template>
  <el-card>
    <h3>告警配置</h3>
    
    <el-form :model="configForm" label-width="150px">
      <!-- CPM突增告警配置 -->
      <el-divider>CPM突增告警</el-divider>
      <el-form-item label="上升率阈值">
        <el-input-number v-model="configForm.cpmRise.risePercentage" :min="0.01" :max="1" :step="0.01" />
        <span>%</span>
      </el-form-item>
      <el-form-item label="最小检查间隔">
        <el-input-number v-model="configForm.cpmRise.minInterval" :min="60" :step="60" />
        <span>秒</span>
      </el-form-item>
      <el-form-item label="最小CPM基数">
        <el-input-number v-model="configForm.cpmRise.minCpm" :min="0" />
      </el-form-item>
      
      <!-- 低电压告警配置 -->
      <el-divider>低电压告警</el-divider>
      <el-form-item label="电压阈值">
        <el-input-number v-model="configForm.lowBattery.voltageThreshold" :min="0" :step="0.1" />
        <span>V</span>
      </el-form-item>
      
      <!-- 离线超时配置 -->
      <el-divider>设备离线告警</el-divider>
      <el-form-item label="离线超时时间">
        <el-input-number v-model="configForm.offlineTimeout.timeoutMinutes" :min="1" />
        <span>分钟</span>
      </el-form-item>
    </el-form>
    
    <el-button type="primary" @click="saveConfig">保存配置</el-button>
  </el-card>
</template>
```

---

## 实施计划

### Phase 1：核心功能（必须）

1. ✅ 创建AlertConfig实体
2. ✅ Device实体添加lastMessageAt字段
3. ✅ 更新AlertType枚举
4. ⬜ 编写AlertConfigService
5. ⬜ 重构AlertService的告警检查逻辑
6. ⬜ 修改MqttMessageListener更新lastMessageAt
7. ⬜ 创建设备离线检查定时任务

### Phase 2：配置管理（推荐）

8. ⬜ 编写AlertConfigController
9. ⬜ 实现前端配置页面
10. ⬜ 添加配置校验逻辑

### Phase 3：优化增强（可选）

11. ⬜ 告警历史记录
12. ⬜ 告警趋势分析
13. ⬜ 配置导入导出

---

## 关键代码变更清单

### 需要修改的文件

1. ✅ `backend/src/main/java/com/cdutetc/ems/entity/AlertConfig.java` - 新建
2. ✅ `backend/src/main/java/com/cdutetc/ems/entity/Device.java` - 添加字段
3. ✅ `backend/src/main/java/com/cdutetc/ems/entity/enums/AlertType.java` - 更新
4. ⬜ `backend/src/main/java/com/cdutetc/ems/service/AlertConfigService.java` - 新建
5. ⬜ `backend/src/main/java/com/cdutetc/ems/service/AlertService.java` - 重构
6. ⬜ `backend/src/main/java/com/cdutetc/ems/mqtt/MqttMessageListener.java` - 更新
7. ⬜ `backend/src/main/java/com/cdutetc/ems/scheduler/DeviceOfflineCheckScheduler.java` - 新建
8. ⬜ `backend/src/main/java/com/cdutetc/ems/controller/AlertConfigController.java` - 新建

---

## 测试方案

### 单元测试

1. **CPM上升率计算测试**
```java
@Test
void testCpmRiseRateCalculation() {
    // 上次100，当前120，上升率应该为20%
    double lastCpm = 100.0;
    double currentCpm = 120.0;
    double riseRate = (currentCpm - lastCpm) / lastCpm;
    assertEquals(0.2, riseRate, 0.001);
}
```

2. **离线检查测试**
```java
@Test
void testDeviceOfflineCheck() {
    // 设备最后消息时间11分钟前
    device.setLastMessageAt(LocalDateTime.now().minusMinutes(11));
    // 配置超时10分钟
    // 预期：触发离线告警
}
```

### 集成测试

1. **端到端告警测试**
   - 发送MQTT消息
   - 验证lastMessageAt更新
   - 验证告警创建
   - 验证SSE推送

---

*方案设计时间：2025-12-27*
*设计人：Claude Code*
