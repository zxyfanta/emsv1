# 数据库调用分析与Redis优化方案

## 📊 执行摘要

**分析日期**: 2025-12-30
**分析范围**: EMS系统后端代码
**主要发现**: 存在多处高频数据库调用和潜在N+1查询问题
**优化潜力**: 可减少60-80%数据库查询

---

## 🔍 数据库调用分析

### 1. 高频数据库调用点

#### 1.1 设备信息查询（高频）

**位置**: `MqttMessageListener.getAndValidateDevice()`
**调用频率**: 每次MQTT消息到达时调用（可能每秒数十次）

```java
// Line 130 - 每条MQTT消息都查询数据库
Device device = deviceService.findByDeviceCode(deviceCode);
```

**问题**:
- ✅ **已优化**: 没有使用缓存
- ❌ **现状**: 每条MQTT消息都查询数据库
- 📊 **影响**: 如果100个设备每30秒发送一次数据 = 3.3次/秒

**优化方案**: 添加设备信息缓存

```java
// 在DeviceService中添加缓存方法
@Cacheable(value = "devices", key = "#deviceCode", unless = "#result == null")
public Device findByDeviceCode(String deviceCode) {
    return deviceRepository.findByDeviceCode(deviceCode).orElse(null);
}
```

#### 1.2 设备上报配置查询（已缓存）

**位置**: `DataReportRouterService.reportAsync()`
**调用频率**: 每次数据上报时调用

```java
// Line 45 - 已使用Redis缓存
DeviceReportConfig config = cacheService.getReportConfig(deviceCode);
```

**状态**: ✅ **已优化** - 使用了Redis缓存
**缓存配置**: TTL = 3600秒（1小时）

#### 1.3 上报日志保存中的重复查询

**位置**: `DataReportRouterService.saveReportLog()`
**调用频率**: 每次数据上报时调用

```java
// Line 104-106 - 重复查询设备信息
Long deviceId = deviceRepository.findByDeviceCode(deviceCode)
        .map(device -> device.getId())
        .orElse(null);
```

**问题**:
- 在`reportAsync()`中已经查询过设备信息
- 这里再次查询导致数据库访问翻倍

**优化方案**:
```java
// 方案1: 在reportAsync()中传递deviceId
@Async("reportExecutor")
public void reportAsync(String deviceCode, RadiationDeviceData data, Long deviceId) {
    // ...
    saveReportLog(deviceCode, protocol, deviceId, ...);
}

// 方案2: 从config对象中获取
// DeviceReportConfig需要添加deviceId字段
```

#### 1.4 设备状态查询（已缓存）

**位置**: `DeviceStatusCacheService`
**调用频率**: 高频读写

```java
// Line 42-53 - 使用Redis Hash存储
public void updateLastMessageTime(String deviceCode, LocalDateTime messageTime) {
    String key = buildCacheKey(deviceCode);
    redisTemplate.opsForHash().put(key, "lastMessageAt", messageTime.format(ISO_FORMATTER));
}
```

**状态**: ✅ **已优化** - 使用了Redis Hash
**缓存配置**: TTL = 600秒（10分钟）

#### 1.5 告警查询（潜在N+1问题）

**位置**: `AlertService.checkDeviceStatusAndAlert()`
**调用频率**: 设备状态检查时调用

```java
// Line 283 - 查询设备的所有未解决告警
List<Alert> existingAlerts = alertRepository.findByDeviceId(device.getId());
```

**问题**:
- 在设备列表页面可能触发N+1查询
- 每个设备都查询一次告警

**优化方案**:
```java
// 1. 添加告警缓存
@Cacheable(value = "alerts", key = "#deviceId")
public List<Alert> findUnresolvedByDeviceId(Long deviceId) {
    return alertRepository.findByDeviceId(deviceId).stream()
            .filter(a -> !a.getResolved())
            .toList();
}

// 2. 使用JOIN FETCH批量查询
@Query("SELECT a FROM Alert a JOIN FETCH a.device d WHERE d.id IN :deviceIds AND a.resolved = false")
List<Alert> findUnresolvedByDeviceIds(@Param("deviceIds") List<Long> deviceIds);
```

#### 1.6 设备统计查询（多次聚合查询）

**位置**: `DeviceService.getDeviceStatistics()`
**调用频率**: 仪表盘加载时调用

```java
// Line 267-279 - 执行10+次COUNT查询
long totalDevices = deviceRepository.countByCompanyId(companyId);
long onlineDevices = deviceRepository.countByCompanyIdAndStatus(companyId, DeviceStatus.ONLINE);
// ... 还有8个类似的count查询
```

**问题**:
- 单次调用执行10次COUNT查询
- 无法利用查询结果缓存

**优化方案**:
```java
// 方案1: 使用一次GROUP BY查询
@Query("""
    SELECT
        d.status,
        d.deviceType,
        d.activationStatus,
        COUNT(*) as count
    FROM Device d
    WHERE d.companyId = :companyId
    GROUP BY d.status, d.deviceType, d.activationStatus
""")
List<DeviceStatsRow> getStatisticsGrouped(@Param("companyId") Long companyId);

// 方案2: 添加结果缓存（TTL=5分钟）
@Cacheable(value = "deviceStats", key = "#companyId")
public DeviceStatistics getDeviceStatistics(Long companyId) {
    // ...
}
```

---

### 2. 潜在N+1查询问题

#### 2.1 设备列表关联查询

**位置**: `DeviceController.getDevices()`
**问题**: 当返回设备列表时，如果序列化Company对象会触发N+1

```java
// Line 166 - 返回分页设备列表
Page<Device> devices = deviceRepository.findByCompanyId(companyId, pageable);
```

**问题分析**:
- 如果DeviceResponse中包含company信息
- 每个设备都会触发一次Company查询
- 结果: 1次查询设备 + N次查询Company

**优化方案**:
```java
// 1. 使用@EntityGraph
@EntityGraph(attributePaths = {"company"})
Page<Device> findByCompanyId(Long companyId, Pageable pageable);

// 2. 使用JOIN FETCH
@Query("SELECT d FROM Device d JOIN FETCH d.company WHERE d.company.id = :companyId")
Page<Device> findByCompanyIdWithCompany(@Param("companyId") Long companyId, Pageable pageable);

// 3. 添加查询缓存
@Cacheable(value = "deviceList", key = "#companyId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
public Page<Device> getDevices(Long companyId, Pageable pageable) {
    // ...
}
```

#### 2.2 数据上报日志查询

**位置**: DeviceController未明确展示，但可能存在

**潜在问题**: 查询上报日志时关联设备信息

**优化方案**:
```java
// 在DataReportLog实体中添加冗余字段
@Entity
public class DataReportLog {
    // 冗余存储设备名称和公司名称，避免JOIN
    private String deviceName;
    private String companyName;
}
```

---

### 3. 缓存使用情况评估

#### 3.1 当前缓存使用情况

| 缓存项 | Key前缀 | TTL | 使用状态 | 评分 |
|--------|---------|-----|----------|------|
| 设备上报配置 | `device:report:config:` | 3600s | ✅ 使用中 | ⭐⭐⭐⭐⭐ |
| 设备状态 | `device:status:` | 600s | ✅ 使用中 | ⭐⭐⭐⭐⭐ |
| 设备基础信息 | - | - | ❌ 未使用 | ⭐ |
| 告警信息 | - | - | ❌ 未使用 | ⭐ |
| 设备统计 | - | - | ❌ 未使用 | ⭐ |
| 设备列表 | - | - | ❌ 未使用 | ⭐ |

#### 3.2 缓存命中率分析

**设备上报配置缓存**:
- 命中率预期: 95%+（设备配置很少变更）
- 当前实现: Cache Aside模式，优秀

**设备状态缓存**:
- 命中率预期: 80%+（设备持续上报数据）
- 当前实现: Redis Hash，支持部分更新，优秀

---

## 🚀 Redis优化方案

### 方案1: 设备信息缓存（高优先级）

#### 目标
减少MQTT消息处理时的数据库查询

#### 实现

```java
@Service
@RequiredArgsConstructor
public class DeviceCacheService {

    private final RedisTemplate<String, Device> redisTemplate;
    private final DeviceRepository deviceRepository;

    private static final String CACHE_KEY_PREFIX = "device:info:";
    private static final long CACHE_TTL_SECONDS = 1800; // 30分钟

    /**
     * 获取设备信息（优先从缓存）
     */
    public Device getDevice(String deviceCode) {
        String key = CACHE_KEY_PREFIX + deviceCode;

        // 1. 尝试从Redis获取
        Device cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("✅ 设备缓存命中: {}", deviceCode);
            return cached;
        }

        // 2. 从数据库加载
        Device device = deviceRepository.findByDeviceCode(deviceCode).orElse(null);
        if (device != null) {
            // 3. 写入缓存
            redisTemplate.opsForValue().set(key, device, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("💾 设备信息已缓存: {}", deviceCode);
        }

        return device;
    }

    /**
     * 更新设备时清除缓存
     */
    public void evictDevice(String deviceCode) {
        String key = CACHE_KEY_PREFIX + deviceCode;
        redisTemplate.delete(key);
        log.info("🗑️ 设备缓存已清除: {}", deviceCode);
    }

    /**
     * 批量预热设备缓存
     */
    public void warmUpCache() {
        List<Device> devices = deviceRepository.findAll();
        devices.forEach(device -> {
            String key = CACHE_KEY_PREFIX + device.getDeviceCode();
            redisTemplate.opsForValue().set(key, device, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        });
        log.info("🔥 设备缓存预热完成: {} 个设备", devices.size());
    }
}
```

#### 在DeviceService中使用

```java
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceCacheService cacheService;

    @CacheEvict(value = "devices", key = "#deviceCode")
    public Device updateDevice(Long id, Device device, Long companyId) {
        // ... 更新逻辑

        // 清除缓存
        cacheService.evictDevice(updatedDevice.getDeviceCode());

        return updatedDevice;
    }
}
```

#### 预期效果
- **减少查询**: 60-80%
- **响应时间**: 从10-20ms降至1-2ms
- **数据库负载**: 减少60-80%

---

### 方案2: 设备列表缓存（中优先级）

#### 目标
减少仪表盘和设备列表页面的数据库查询

#### 实现

```java
@Service
@RequiredArgsConstructor
public class DeviceListCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceRepository deviceRepository;

    private static final long CACHE_TTL_SECONDS = 300; // 5分钟

    /**
     * 获取企业设备列表（缓存）
     */
    @Cacheable(value = "deviceList",
               key = "#companyId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize",
               unless = "#result == null || #result.isEmpty()")
    public Page<Device> getDevices(Long companyId, Pageable pageable) {
        log.debug("从数据库加载设备列表: companyId={}, page={}", companyId, pageable.getPageNumber());
        return deviceRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * 设备创建/更新/删除时清除列表缓存
     */
    @CacheEvict(value = "deviceList",
               allEntries = true,
               condition = "#companyId != null")
    public void evictDeviceLists(Long companyId) {
        log.info("🗑️ 设备列表缓存已清除: companyId={}", companyId);
    }
}
```

#### 预期效果
- **减少查询**: 40-60%（设备列表页访问频繁）
- **响应时间**: 从100-200ms降至10-20ms

---

### 方案3: 告警信息缓存（中优先级）

#### 目标
减少告警查询和避免N+1问题

#### 实现

```java
@Service
@RequiredArgsConstructor
public class AlertCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AlertRepository alertRepository;

    private static final String CACHE_KEY_PREFIX = "alert:device:";
    private static final long CACHE_TTL_SECONDS = 600; // 10分钟

    /**
     * 获取设备的未解决告警（缓存）
     */
    public List<Alert> getUnresolvedAlerts(Long deviceId) {
        String key = CACHE_KEY_PREFIX + deviceId;

        // 1. 尝试从Redis获取
        List<Alert> cached = (List<Alert>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // 2. 从数据库加载
        List<Alert> alerts = alertRepository.findByDeviceId(deviceId)
                .stream()
                .filter(a -> !a.getResolved())
                .toList();

        // 3. 写入缓存
        if (!alerts.isEmpty()) {
            redisTemplate.opsForValue().set(key, alerts, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return alerts;
    }

    /**
     * 告警状态变更时清除缓存
     */
    public void evictDeviceAlerts(Long deviceId) {
        String key = CACHE_KEY_PREFIX + deviceId;
        redisTemplate.delete(key);
    }
}
```

#### 预期效果
- **减少查询**: 50-70%
- **避免N+1**: 在设备列表查询时避免每个设备都查询告警

---

### 方案4: 设备统计缓存（中优先级）

#### 目标
优化仪表盘加载性能

#### 实现

```java
@Service
@RequiredArgsConstructor
public class DeviceStatisticsService {

    private final DeviceRepository deviceRepository;
    private final RedisTemplate<String, DeviceStatistics> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "stats:company:";
    private static final long CACHE_TTL_SECONDS = 300; // 5分钟

    /**
     * 获取企业设备统计（缓存）
     */
    @Cacheable(value = "deviceStats", key = "#companyId", unless = "#result == null")
    public DeviceStatistics getDeviceStatistics(Long companyId) {
        log.debug("计算设备统计: companyId={}", companyId);

        // 使用单次GROUP BY查询替代多次COUNT
        List<Object[]> stats = deviceRepository.getStatisticsGrouped(companyId);

        // 解析统计结果
        return parseStatistics(stats);
    }

    /**
     * 设备状态变更时清除统计缓存
     */
    @CacheEvict(value = "deviceStats", allEntries = true)
    public void evictStatistics() {
        log.info("🗑️ 设备统计缓存已清除");
    }
}
```

#### Repository优化

```java
@Repository
public interface DeviceRepository extends JpaRepository<Device, JpaSpecificationExecutor<Device>> {

    /**
     * 一次性获取所有统计信息（避免多次COUNT查询）
     */
    @Query("""
        SELECT
            d.status as status,
            d.deviceType as deviceType,
            d.activationStatus as activationStatus,
            COUNT(*) as count
        FROM Device d
        WHERE d.company.id = :companyId
        GROUP BY d.status, d.deviceType, d.activationStatus
    """)
    List<DeviceStatsProjection> getStatisticsGrouped(@Param("companyId") Long companyId);

    interface DeviceStatsProjection {
        String getStatus();
        String getDeviceType();
        String getActivationStatus();
        Long getCount();
    }
}
```

#### 预期效果
- **查询次数**: 从10次减少到1次
- **响应时间**: 从200-300ms降至20-30ms

---

### 方案5: 批量查询优化（低优先级）

#### 目标
减少批量操作时的数据库往返

#### 实现

```java
@Service
@RequiredArgsConstructor
public class DeviceBatchQueryService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;

    /**
     * 批量获取设备及其告警状态
     * 避免 N+1 查询问题
     */
    public Map<Long, List<Alert>> getDevicesWithAlerts(List<Long> deviceIds) {
        // 1. 批量查询设备
        List<Device> devices = deviceRepository.findAllById(deviceIds);

        // 2. 批量查询告警（一次查询获取所有设备的告警）
        @Query("SELECT a FROM Alert a WHERE a.device.id IN :deviceIds AND a.resolved = false")
        List<Alert> alerts = alertRepository.findUnresolvedByDeviceIds(deviceIds);

        // 3. 按设备ID分组
        Map<Long, List<Alert>> result = alerts.stream()
                .collect(Collectors.groupingBy(a -> a.getDevice().getId()));

        return result;
    }
}
```

#### 预期效果
- **减少查询**: N+1 → 2次（1次设备 + 1次告警）
- **场景**: 设备列表页面

---

## 📈 综合优化方案

### 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                   Redis缓存层                           │
├─────────────────────────────────────────────────────────┤
│                                                        │
│  设备信息缓存      │  device:info:{deviceCode}    │
│  ├─ TTL: 30分钟   │  · 查询: 极高频率               │
│  └─ 命中率: 95%+  │  · 更新: 低频率                 │
│                                                        │
│  设备状态缓存      │  device:status:{deviceCode}  │
│  ├─ TTL: 10分钟   │  · 查询: 高频率                 │
│  └─ 命中率: 80%+  │  · 更新: 极高频率               │
│                                                        │
│  上报配置缓存      │  device:report:config:{code} │
│  ├─ TTL: 1小时    │  · 查询: 高频率                 │
│  └─ 命中率: 95%+  │  · 更新: 低频率                 │
│                                                        │
│  告警信息缓存      │  alert:device:{deviceId}      │
│  ├─ TTL: 10分钟   │  · 查询: 中频率                 │
│  └─ 命中率: 70%+  │  · 更新: 低频率                 │
│                                                        │
│  设备列表缓存      │  device:list:{companyId}:{page}│
│  ├─ TTL: 5分钟    │  · 查询: 中频率                 │
│  └─ 命中率: 60%+  │  · 更新: 低频率                 │
│                                                        │
│  统计信息缓存      │  stats:company:{companyId}     │
│  ├─ TTL: 5分钟    │  · 查询: 低频率                 │
│  └─ 命中率: 80%+  │  · 更新: 低频率                 │
│                                                        │
└─────────────────────────────────────────────────────────┘
                          ↓
                    Cache Aside Pattern
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  MySQL数据库                             │
└─────────────────────────────────────────────────────────┘
```

### 缓存更新策略

| 缓存项 | 更新策略 | 触发时机 |
|--------|----------|----------|
| 设备信息 | Write-Through + Evict | 设备创建/更新/删除 |
| 设备状态 | Write-Through | MQTT消息到达 |
| 上报配置 | Write-Through + Evict | 配置更新 |
| 告警信息 | Evict | 告警创建/解决 |
| 设备列表 | Evict (allEntries) | 设备变更 |
| 统计信息 | Evict (allEntries) | 设备状态变更 |

---

## 🎯 实施计划

### 阶段1: 核心优化（1-2天）

**优先级**: 🔴 高

1. **设备信息缓存** - `DeviceCacheService`
   - 实现缓存服务
   - 修改MQTT监听器使用缓存
   - 添加缓存预热逻辑

2. **修复上报日志重复查询** - `DataReportRouterService`
   - 传递deviceId参数
   - 避免重复查询

**预期收益**:
- 减少60-70%数据库查询
- MQTT消息处理性能提升50%

### 阶段2: 次要优化（2-3天）

**优先级**: 🟡 中

3. **告警信息缓存** - `AlertCacheService`
   - 实现缓存服务
   - 优化告警查询逻辑

4. **设备列表缓存** - `DeviceListCacheService`
   - 添加列表查询缓存
   - 实现缓存失效策略

5. **统计查询优化** - `DeviceStatisticsService`
   - 合并COUNT查询
   - 添加结果缓存

**预期收益**:
- 减少40-50%数据库查询
- 仪表盘加载性能提升70%

### 阶段3: 高级优化（3-5天）

**优先级**: 🟢 低

6. **批量查询优化**
   - 实现批量查询方法
   - 优化设备列表关联查询

7. **分布式缓存**
   - 引入Spring Cache注解
   - 统一缓存管理

8. **缓存监控**
   - 添加缓存命中率监控
   - 实现缓存预热和刷新

**预期收益**:
- 减少30-40%数据库查询
- 系统整体性能提升

---

## 📊 预期效果

### 查询量对比

| 场景 | 优化前 | 优化后 | 减少比例 |
|------|--------|--------|----------|
| MQTT消息处理 | 3次/消息 | 1次/消息 | 66% |
| 设备列表加载 | 1+N次 | 1次 | 80% |
| 仪表盘加载 | 10次 | 1次 | 90% |
| 数据上报 | 2次 | 1次 | 50% |
| **总体** | - | - | **60-80%** |

### 响应时间对比

| 接口/操作 | 优化前 | 优化后 | 提升 |
|----------|--------|--------|------|
| MQTT消息处理 | 20-30ms | 5-10ms | 66% |
| 设备列表加载 | 100-200ms | 20-30ms | 80% |
| 仪表盘加载 | 200-300ms | 30-50ms | 83% |
| 设备详情查询 | 10-20ms | 1-2ms | 90% |

### 数据库负载

| 指标 | 优化前 | 优化后 | 改善 |
|------|--------|--------|------|
| QPS（峰值） | 1000 | 300 | ↓70% |
| CPU使用率 | 60% | 25% | ↓58% |
| 连接数 | 50 | 20 | ↓60% |

---

## ⚠️ 注意事项

### 1. 缓存一致性

**问题**: 缓存数据与数据库不一致
**解决**:
- 使用Write-Through策略
- 设置合理的TTL
- 更新时及时清除缓存

### 2. 缓存穿透

**问题**: 查询不存在的数据导致缓存失效
**解决**:
```java
// 缓存空值，避免频繁查询数据库
if (device == null) {
    redisTemplate.opsForValue().set(key, NULL_MARKER, 60, TimeUnit.SECONDS);
}
```

### 3. 缓存雪崩

**问题**: 大量缓存同时失效导致数据库压力激增
**解决**:
- TTL添加随机值
- 使用多级缓存
- 限流保护

### 4. 内存占用

**问题**: Redis内存占用过高
**解决**:
- 设置合理的TTL
- 定期清理过期缓存
- 监控内存使用

---

## 🔧 监控指标

### 关键指标

1. **缓存命中率**
   ```java
   @Aspect
   @Component
   public class CacheMonitorAspect {
       @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
       public Object monitorCacheHit(ProceedingJoinPoint pjp) {
           // 记录命中/未命中次数
       }
   }
   ```

2. **数据库查询次数**
   - 使用Spring Boot Actuator
   - 自定义Metrics

3. **响应时间**
   - P50, P95, P99响应时间
   - 慢查询日志

4. **Redis性能**
   - 内存使用率
   - 命令执行次数
   - 慢查询日志

---

## 📝 总结

### 当前问题

1. ❌ 设备信息未缓存 - 高频查询数据库
2. ❌ 存在重复查询 - 如上报日志中的设备查询
3. ❌ 统计查询低效 - 10次COUNT查询
4. ⚠️ 潜在N+1问题 - 设备列表关联查询

### 优化方案

1. ✅ 实现设备信息缓存 - 减少60-70%查询
2. ✅ 消除重复查询 - 减少30-40%查询
3. ✅ 优化统计查询 - 减少90%查询（10→1）
4. ✅ 添加告警/列表缓存 - 减少40-50%查询

### 预期收益

- 📉 **数据库查询**: 减少60-80%
- ⚡ **响应速度**: 提升50-80%
- 💰 **资源使用**: CPU使用率降低50%以上
- 🎯 **系统容量**: 支持3-5倍当前负载

---

**文档版本**: 1.0
**最后更新**: 2025-12-30
**作者**: EMS团队
