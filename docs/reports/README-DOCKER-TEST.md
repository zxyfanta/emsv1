# EMS Docker测试环境使用指南

## 🐳 概述

本指南介绍如何使用Docker Compose启动完整的EMS测试环境，包括Spring Boot后端、Mosquitto MQTT Broker和Node-RED设备模拟器。

## 🏗️ 架构组件

```
┌─────────────────┐    MQTT     ┌─────────────────┐    HTTP     ┌─────────────────┐
│                 │ ◄─────────► │                 │ ◄─────────► │                 │
│   Node-RED      │             │  Mosquitto      │             │  EMS Backend    │
│ 设备模拟器       │             │ MQTT Broker     │             │ Spring Boot     │
│ (端口: 1880)    │             │ (端口: 1883)    │             │ (端口: 8081)    │
└─────────────────┘             └─────────────────┘             └─────────────────┘
        │                               │                               │
        └───────────────────────────────┴───────────────────────────────┘
                                    Docker Network
```

## 🚀 快速启动

### 1. 环境准备

确保已安装以下软件：
- Docker Desktop (Windows/Mac) 或 Docker Engine (Linux)
- Docker Compose

### 2. 启动服务

```bash
# 在emsv1根目录执行
docker-compose up -d
```

### 3. 验证服务状态

```bash
# 查看所有服务状态
docker-compose ps

# 查看EMS后端日志
docker-compose logs ems-backend

# 查看Node-RED日志
docker-compose logs nodered

# 查看MQTT Broker日志
docker-compose logs mosquitto
```

### 4. 等待服务就绪

所有服务启动需要1-2分钟，等待健康检查通过：
- ✅ ems-backend: 健康检查通过
- ✅ ems-nodered: 健康检查通过
- ✅ ems-mosquitto: 健康检查通过

## 🌐 访问地址

| 服务 | 地址 | 描述 |
|------|------|------|
| EMS后端API | http://localhost:8081/api | Spring Boot REST API |
| H2数据库控制台 | http://localhost:8081/api/h2-console | 数据库管理界面 |
| Node-RED编辑器 | http://localhost:1880 | 设备模拟器流程编辑器 |
| MQTT Broker | localhost:1883 | MQTT消息代理 |

## 📊 Node-RED设备模拟器

### 设备配置

Node-RED预配置了以下测试设备：

| 设备ID | 类型 | 状态 | 发送频率 | MQTT主题 |
|--------|------|------|----------|----------|
| RAD-001 | 辐射设备 | 已注册 | 每30秒 | `ems/device/RAD-001/data/RADIATION` |
| ENV-001 | 环境设备 | 已注册 | 每45秒 | `ems/device/ENV-001/data/ENVIRONMENT` |
| RAD-999 | 辐射设备 | 未注册 | 每60秒 | `ems/device/RAD-999/data/RADIATION` |
| ENV-999 | 环境设备 | 未注册 | 每90秒 | `ems/device/ENV-999/data/ENVIRONMENT` |

### 数据格式示例

**辐射设备数据 (RAD-001):**
```json
{
    "BDS": {
        "longitude": "11607.4321",
        "latitude": "3998.7654",
        "useful": 1,
        "UTC": "2025/12/22 14:30:00"
    },
    "CPM": 35,
    "Batvolt": 4000,
    "signal": 4,
    "temperature": 22.5,
    "time": "2025/12/22 14:30:00"
}
```

**环境设备数据 (ENV-001):**
```json
{
    "src": 1,
    "CPM": 8,
    "temperature": 25.6,
    "wetness": 68.5,
    "windspeed": 3.2,
    "total": 85.3,
    "battery": 11.8
}
```

## 🧪 测试验证

### 1. 设备注册测试

首先需要注册测试设备：

```bash
# 注册RAD-001设备
curl -X POST http://localhost:8081/api/devices \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "RAD-001",
    "deviceName": "测试辐射设备001",
    "deviceType": "RADIATION",
    "description": "Node-RED测试用辐射设备"
  }'

# 注册ENV-001设备
curl -X POST http://localhost:8081/api/devices \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "ENV-001",
    "deviceName": "测试环境设备001",
    "deviceType": "ENVIRONMENT",
    "description": "Node-RED测试用环境设备"
  }'
```

### 2. 查看Node-RED数据流

1. 访问 http://localhost:1880
2. 查看右侧调试面板
3. 观察设备数据实时输出
4. 验证数据格式和发送频率

### 3. 验证EMS后端接收

```bash
# 查看EMS后端日志
docker-compose logs -f ems-backend

# 检查数据库中的设备数据
# 访问 http://localhost:8081/api/h2-console
# JDBC URL: jdbc:h2:mem:ems-docker-db
# 执行查询: SELECT * FROM radiation_device_data ORDER BY created_at DESC LIMIT 10;
```

### 4. API测试

```bash
# 测试健康检查
curl http://localhost:8081/api/actuator/health

# 查看设备列表
curl http://localhost:8081/api/devices

# 手动发送设备数据（模拟Node-RED）
curl -X POST http://localhost:8081/api/device-data/radiation \
  -H "Content-Type: application/json" \
  -d '{
    "BDS": {
      "longitude": "11607.4321",
      "latitude": "3998.7654",
      "useful": 1
    },
    "CPM": 42,
    "Batvolt": 3950,
    "time": "2025/12/22 15:00:00"
  }'
```

## 🔧 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 重新构建并启动
   docker-compose down
   docker-compose up -d --build
   ```

2. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -an | grep 8081
   netstat -an | grep 1880
   netstat -an | grep 1883

   # 停止占用端口的进程
   # Windows: taskkill /PID <PID> /F
   # Linux/Mac: kill -9 <PID>
   ```

3. **Node-RED无法连接MQTT**
   ```bash
   # 检查MQTT Broker状态
   docker-compose logs mosquitto

   # 验证MQTT连接
   docker exec ems-nodered npm install -g mosquitto-clients
   docker exec ems-nodered mosquitto_pub -h mosquitto -t test -m "hello"
   ```

4. **EMS后端无法接收数据**
   ```bash
   # 检查后端日志
   docker-compose logs ems-backend

   # 验证API可访问性
   curl http://localhost:8081/api/actuator/health
   ```

### 重置环境

```bash
# 完全重置（删除所有数据和容器）
docker-compose down -v
docker system prune -f
docker-compose up -d
```

## 📝 开发调试

### 修改Node-RED流程

1. 访问 http://localhost:1880
2. 修改设备数据生成逻辑
3. 调整发送频率和数据格式
4. 部署更新后的流程

### 调整EMS配置

修改 `backend/src/main/resources/application-docker.yaml` 中的配置：
- 日志级别
- 数据库设置
- JWT配置
- 业务参数

### 自定义测试场景

1. 在Node-RED中添加新的测试设备
2. 修改设备数据生成函数
3. 创建不同的测试场景流程
4. 验证EMS系统响应

## 🎯 测试目标

通过这个Docker测试环境，可以验证：

✅ **设备数据接收** - Node-RED到EMS后端的数据传输
✅ **设备认证** - 已注册vs未注册设备的处理差异
✅ **数据存储** - 数据在H2数据库中的正确存储
✅ **业务逻辑** - 设备数据处理的正确性
✅ **API功能** - REST API的完整功能
✅ **系统稳定性** - 长时间运行的稳定性
✅ **错误处理** - 异常情况的处理能力

## 📚 相关文档

- [Node-RED官方文档](https://nodered.org/docs/)
- [Mosquitto MQTT文档](https://mosquitto.org/documentation/)
- [Spring Boot Docker指南](https://spring.io/guides/gs/spring-boot-docker/)
- [EMS系统架构文档](./docs/architecture.md)

---

**注意**: 这是测试环境，请勿在生产环境中使用相同的配置。