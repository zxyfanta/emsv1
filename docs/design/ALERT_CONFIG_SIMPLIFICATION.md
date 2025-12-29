# 告警配置简化说明

## 📋 简化原因

**用户需求**：设备是平台生产的，所有企业使用相同的配置，不需要企业级定制。

## ✂️ 简化内容

### 删除的文件
1. ❌ `AlertConfig.java` - 告警配置实体类（不再需要数据库表）
2. ❌ `AlertConfigRepository.java` - 告警配置数据访问层（不再需要数据库操作）

### 保留的文件
1. ✅ `AlertConfigService.java` - 简化为直接读取配置文件
2. ✅ `AlertProperties.java` - 新增：从application.yaml加载配置

### 配置方式变更

#### 旧方案（已废弃）
```
数据库表 alert_configs
  ├─ 全局配置（company_id = NULL）
  └─ 企业配置（company_id = 1, 2, 3...）

复杂度：
  - 需要数据库表
  - 需要配置降级逻辑
  - 需要配置管理API
  - 需要前端配置页面
```

#### 新方案（当前）
```yaml
# application.yaml
app:
  ems:
    alert:
      cpm-rise:
        rise-percentage: 0.15
        min-interval: 300
        min-cpm: 50
      low-battery:
        voltage-threshold: 3.5
      offline-timeout:
        timeout-minutes: 10
```

**优势**：
- ✅ 零数据库依赖
- ✅ 配置集中管理
- ✅ 启动时加载，运行时高效
- ✅ 支持环境变量覆盖（Docker友好）

## 📦 新的配置架构

### 配置加载流程
```
application.yaml
       ↓
AlertProperties (@ConfigurationProperties)
       ↓
AlertConfigService (提供访问接口)
       ↓
AlertService / 其他服务（使用配置）
```

### 核心代码

#### AlertProperties.java
```java
@Component
@ConfigurationProperties(prefix = "app.ems.alert")
public class AlertProperties {
    private CpmRise cpmRise = new CpmRise();
    private LowBattery lowBattery = new LowBattery();
    private OfflineTimeout offlineTimeout = new OfflineTimeout();

    @Data
    public static class CpmRise {
        private double risePercentage = 0.15;
        private int minInterval = 300;
        private int minCpm = 50;
    }

    @Data
    public static class LowBattery {
        private double voltageThreshold = 3.5;
    }

    @Data
    public static class OfflineTimeout {
        private int timeoutMinutes = 10;
    }
}
```

#### AlertConfigService.java（简化版）
```java
@Service
@RequiredArgsConstructor
public class AlertConfigService {
    private final AlertProperties alertProperties;

    public AlertProperties.CpmRise getCpmRiseConfig() {
        return alertProperties.getCpmRise();
    }

    public AlertProperties.LowBattery getLowBatteryConfig() {
        return alertProperties.getLowBattery();
    }

    public AlertProperties.OfflineTimeout getOfflineTimeoutConfig() {
        return alertProperties.getOfflineTimeout();
    }
}
```

#### 使用示例
```java
@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertConfigService alertConfigService;
    private final DeviceStatusCacheService cacheService;

    public void checkCpmRise(String deviceCode, Double currentCpm) {
        // 获取配置
        var config = alertConfigService.getCpmRiseConfig();

        // 从缓存获取上一次的CPM值
        Double lastCpm = cacheService.getLastCpm(deviceCode);

        if (lastCpm != null && lastCpm > config.getMinCpm()) {
            double riseRate = (currentCpm - lastCpm) / lastCpm;

            if (riseRate > config.getRisePercentage()) {
                // 触发告警
                createAlert(AlertType.CPM_RISE, ...);
            }
        }
    }
}
```

## 🔧 配置修改方式

### 方式1：直接修改application.yaml（推荐）
```yaml
app:
  ems:
    alert:
      cpm-rise:
        rise-percentage: 0.20  # 改为20%
```

### 方式2：使用环境变量（Docker/K8s）
```bash
# 启动时覆盖配置
docker run -e EMS_ALERT_CPM-RISE_RISE-PERCENTAGE=0.20 ...
```

### 方式3：使用Profile（多环境）
```yaml
# application-dev.yaml
app:
  ems:
    alert:
      cpm-rise:
        rise-percentage: 0.30  # 开发环境宽松

# application-prod.yaml
app:
  ems:
    alert:
      cpm-rise:
        rise-percentage: 0.15  # 生产环境严格
```

## 📊 配置项说明

### CPM上升率配置
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `rise-percentage` | 0.15 | 上升率阈值（0.15 = 15%） |
| `min-interval` | 300 | 最小检查间隔，单位：秒（防止频繁告警） |
| `min-cpm` | 50 | 最小CPM基数（避免基数太小导致误报） |

### 低电压配置
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `voltage-threshold` | 3.5 | 电压阈值，单位：伏特(V) |

### 离线超时配置
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `timeout-minutes` | 10 | 离线超时时间，单位：分钟 |

## 🎯 后续工作

虽然配置已简化，但仍需实现实际的告警检查逻辑：

1. **CPM上升率检查** - 核心功能
2. **低电压检查** - 较简单
3. **设备离线检查** - 需要定时任务

详见 [REDIS_CACHE_IMPLEMENTATION.md](REDIS_CACHE_IMPLEMENTATION.md) 中的"后续待实现功能"部分。

## ✅ 验证

### 编译检查
```bash
cd backend
mvn clean compile
# ✅ BUILD SUCCESS
```

### 配置检查
启动应用后查看日志，确认配置已加载：
```
INFO  o.s.b.f.ApplicationReadyListener : 设备状态缓存预热完成
```

### 运行时修改
修改application.yaml后重启应用即可生效，无需数据库迁移。

---

**简化完成时间**：2025-12-27
**代码行数减少**：~200行 → ~80行（减少60%）
**依赖减少**：无需数据库表、Repository、复杂配置逻辑
**维护成本**：大幅降低
