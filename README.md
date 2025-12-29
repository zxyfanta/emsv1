# EMS 能源管理系统

> Energy Management System - 辐射与环境设备监控平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-brightgreen.svg)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📖 项目简介

EMS是一个基于Spring Boot和Vue.js的能源管理系统，主要用于监控和管理辐射监测设备和环境监测站。系统支持设备数据采集、实时监控、数据上报到监管平台、告警管理等核心功能。

### 核心功能

- 📡 **设备数据采集**: 支持MQTT和HTTP两种方式接收设备数据
- 📊 **实时数据监控**: 基于SSE的实时数据推送
- 📤 **监管平台上报**: 支持山东协议（HJ/T212-2005）和四川协议
- ⚠️ **智能告警**: 基于阈值的自动告警系统
- 🏢 **多租户管理**: 支持多公司和用户管理
- 🔐 **权限控制**: 基于JWT和角色的访问控制

---

## 🏗️ 系统架构

```
┌─────────────┐    MQTT     ┌─────────────┐
│   Node-RED  │ ◄─────────► │  Mosquitto  │
│  设备模拟器  │             │ MQTT Broker │
└─────────────┘             └──────┬──────┘
                                   │
                    HTTP/MQTT      │
                                   ▼
┌──────────────────────────────────────────┐
│          EMS Backend (Spring Boot)       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  │
│  │ Controller│→│ Service │→│Repository│  │
│  └─────────┘  └─────────┘  └────┬────┘  │
└─────────────────────────────────┼────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
              ┌─────▼─────┐             ┌──────▼──────┐
              │  MySQL    │             │   Redis     │
              │  持久化存储 │             │   缓存层     │
              └───────────┘             └─────────────┘
```

### 技术栈

**后端**:
- Spring Boot 3.5.9
- Spring Security + JWT
- Spring Data JPA
- Eclipse Paho MQTT
- MySQL 8.0
- Redis 7.x

**前端**:
- Vue.js 3.x
- Element Plus
- Pinia
- Vite

**中间件**:
- Mosquitto MQTT Broker
- Node-RED (设备模拟)

---

## 🚀 快速开始

### 环境要求

- Docker & Docker Compose
- Java 17+
- Node.js 18+
- Maven 3.8+

### 1. 启动基础服务

```bash
# 克隆项目
git clone https://github.com/your-org/ems.git
cd ems

# 启动MySQL、Redis、Mosquitto、Node-RED
docker-compose up -d

# 验证服务状态
docker-compose ps
```

### 2. 启动后端

```bash
cd backend

# 安装依赖并启动
mvn clean install
mvn spring-boot:run

# 后端将在 http://localhost:8080 启动
```

### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 前端将在 http://localhost:5173 启动
```

### 4. 访问系统

- **前端界面**: http://localhost:5173
- **后端API**: http://localhost:8080/api
- **H2控制台**: http://localhost:8080/api/h2-console
- **Node-RED**: http://localhost:1880
- **API文档**: http://localhost:8080/swagger-ui.html

---

## 📚 文档导航

### 设计文档

- [系统架构设计](docs/design/系统架构设计.md) - 系统整体架构和模块设计
- [API接口文档](docs/design/API接口文档.md) - REST API接口说明
- [数据流处理说明](docs/design/数据流处理说明.md) - 数据采集和处理流程
- [部署指南](docs/design/部署指南.md) - 部署和运维指南

### 协议文档

- [HJ/T212-2005协议](docs/protocol/hjt212-2005.md) - 环境污染源在线监控协议
- [山东协议扩展](docs/protocol/shandong-protocol.md) - 山东省数据上报扩展
- [四川协议](docs/sichuan-protocol.md) - 四川省数据上报协议

### 测试文档

- [测试脚本说明](tests/README.md) - 测试脚本使用指南
- [协议测试](tests/protocol/README.md) - 协议测试脚本
- [MQTT测试](tests/mqtt/README.md) - MQTT测试脚本

---

## 🧪 测试

### 运行单元测试

```bash
cd backend

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DeviceDataReceiverControllerTest

# 运行特定测试方法
mvn test -Dtest=DeviceDataReceiverControllerTest#testReceiveRadiationDataSuccess
```

### 运行集成测试

```bash
# 使用测试配置文件
mvn test -Dspring.profiles.active=test
```

### 协议测试

```bash
# 测试山东协议数据上报
python3 tests/protocol/test_shandong_quick.py

# 测试MQTT数据接收
python3 tests/mqtt/test_mqtt_publish.py
```

---

## 📦 项目结构

```
ems/
├── backend/                 # Spring Boot后端
│   ├── src/
│   │   └── main/
│   │       ├── java/com/cdutetc/ems/
│   │       │   ├── controller/      # REST控制器
│   │       │   ├── service/         # 业务逻辑
│   │       │   ├── repository/      # 数据访问
│   │       │   ├── entity/          # 实体类
│   │       │   ├── dto/             # 数据传输对象
│   │       │   ├── security/        # 安全配置
│   │       │   ├── mqtt/            # MQTT集成
│   │       │   └── config/          # 配置类
│   │       └── resources/
│   │           ├── application.yaml # 配置文件
│   │           └── data.sql         # 初始化数据
│   └── pom.xml
├── frontend/                # Vue.js前端
│   ├── src/
│   │   ├── views/            # 页面组件
│   │   ├── components/       # 通用组件
│   │   ├── store/            # Pinia状态管理
│   │   ├── router/           # 路由配置
│   │   └── config/           # 前端配置
│   ├── package.json
│   └── vite.config.js
├── nodered/                 # Node-RED流程
│   └── flows/               # 设备模拟流程
├── tests/                   # 测试脚本
│   ├── protocol/            # 协议测试
│   ├── mqtt/                # MQTT测试
│   └── sql/                 # 数据库脚本
├── docs/                    # 文档
│   ├── design/              # 设计文档
│   ├── reports/             # 报告文档
│   └── protocol/            # 协议文档
├── docker-compose.yml       # Docker编排
└── README.md
```

---

## 🔧 配置说明

### 后端配置

主要配置文件: `backend/src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ems_db
    username: ems_user
    password: ems_password
  redis:
    host: localhost
    port: 6379

ems:
  mqtt:
    host: localhost
    port: 1883
    topic-prefix: ems
  data-report:
    shandong:
      host: 221.214.62.118
      port: 20050
    sichuan:
      url: http://59.225.208.12:18085
```

### 环境变量

可以通过环境变量覆盖配置:

```bash
export SERVER_PORT=8080
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ems_db
export EMS_MQTT_HOST=localhost
export EMS_MQTT_PORT=1883
```

---

## 📊 数据上报

### 山东协议

- **服务器**: 221.214.62.118:20050
- **协议**: TCP + HJ/T212-2005
- **数据格式**: 二进制CPM数据包
- **验证**: CRC16校验

### 四川协议

- **服务器**: 59.225.208.12:18085
- **协议**: HTTP + SM2加密
- **数据格式**: JSON
- **加密**: 国密SM2

### 配置数据上报

```bash
# 更新设备上报配置
curl -X PUT http://localhost:8080/api/devices/{deviceCode}/report-config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "dataReportEnabled": true,
    "reportProtocol": "SHANDONG",
    "inspectionMachineNumber": "002162",
    "sourceNumber": "DE25IR006722"
  }'

# 查看上报日志
curl http://localhost:8080/api/devices/{deviceCode}/report-logs \
  -H "Authorization: Bearer <token>"
```

---

## 🛠️ 开发指南

### 代码规范

- 后端遵循阿里巴巴Java开发手册
- 前端遵循Vue.js风格指南
- 使用Lombok减少样板代码
- 统一使用UTF-8编码

### 提交规范

```bash
# 功能开发
git commit -m "feat: 添加设备批量导入功能"

# 问题修复
git commit -m "fix: 修复数据上报时的时区问题"

# 文档更新
git commit -m "docs: 更新部署文档"

# 重构
git commit -m "refactor: 优化设备服务层代码"
```

### 分支策略

- `master` - 主分支，用于生产环境
- `develop` - 开发分支
- `feature/*` - 功能分支
- `bugfix/*` - 修复分支
- `hotfix/*` - 紧急修复分支

---

## 🐛 故障排除

### 常见问题

1. **后端启动失败**
   ```bash
   # 检查端口占用
   lsof -i :8080

   # 检查数据库连接
   docker-compose ps mysql
   docker-compose logs mysql
   ```

2. **MQTT连接失败**
   ```bash
   # 检查Mosquitto状态
   docker-compose ps mosquitto
   docker-compose logs mosquitto

   # 测试MQTT连接
   telnet localhost 1883
   ```

3. **前端无法访问后端API**
   ```bash
   # 检查后端健康状态
   curl http://localhost:8080/api/actuator/health

   # 检查防火墙
   sudo ufw status
   ```

更多问题请参考 [部署指南 - 故障排除](docs/design/部署指南.md#故障排除)

---

## 📞 联系方式

- **项目主页**: https://github.com/your-org/ems
- **问题反馈**: https://github.com/your-org/ems/issues
- **邮箱**: support@example.com

---

## 📄 许可证

Copyright © 2025 [Your Company Name]

Licensed under the MIT License
