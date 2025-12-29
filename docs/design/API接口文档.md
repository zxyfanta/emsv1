# EMS API接口文档

## 📋 文档信息

- **项目**: EMS (Energy Management System)
- **版本**: 1.0
- **更新日期**: 2025-12-30
- **Base URL**: `http://localhost:8080/api`

---

## 🔐 认证说明

### JWT Token认证

大部分接口需要JWT Token认证，请在请求头中携带：

```http
Authorization: Bearer <token>
```

### 获取Token

通过登录接口获取（见下方认证接口）

---

## 📚 接口分类

### 1. 认证接口 (Auth)

#### 1.1 用户登录

```http
POST /api/auth/login
```

**请求体**:
```json
{
  "username": "admin",
  "password": "password"
}
```

**响应**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "admin",
  "role": "ADMIN",
  "companyId": 1
}
```

---

### 2. 设备管理接口 (Device)

#### 2.1 获取所有设备

```http
GET /api/devices
```

**权限**: `ADMIN`, `USER`

**查询参数**:
- `page`: 页码（默认0）
- `size`: 每页数量（默认10）
- `deviceType`: 设备类型（RADIATION_MONITOR/ENVIRONMENT_STATION）
- `status`: 状态（ONLINE/OFFLINE/FAULT/MAINTENANCE）

**响应**:
```json
{
  "content": [
    {
      "id": 1,
      "deviceCode": "865229085145869",
      "deviceName": "山东协议测试辐射设备",
      "deviceType": "RADIATION_MONITOR",
      "status": "ONLINE",
      "dataReportEnabled": true,
      "reportProtocol": "SHANDONG",
      "companyId": 1
    }
  ],
  "totalElements": 12,
  "totalPages": 2,
  "size": 10,
  "number": 0
}
```

#### 2.2 根据设备编码获取设备

```http
GET /api/devices/{deviceCode}
```

**权限**: `ADMIN`, `USER`

**路径参数**:
- `deviceCode`: 设备编码

**响应**:
```json
{
  "id": 1,
  "deviceCode": "865229085145869",
  "deviceName": "山东协议测试辐射设备",
  "deviceType": "RADIATION_MONITOR",
  "status": "ONLINE",
  "dataReportEnabled": true,
  "reportProtocol": "SHANDONG",
  "inspectionMachineNumber": "002162",
  "sourceNumber": "DE25IR006722",
  "sourceType": "02",
  "lastOnlineAt": "2025-12-30T00:00:00"
}
```

#### 2.3 创建设备

```http
POST /api/devices
```

**权限**: `ADMIN`

**请求体**:
```json
{
  "deviceCode": "123456789012345",
  "deviceName": "新辐射设备",
  "deviceType": "RADIATION_MONITOR",
  "companyId": 1,
  "reportProtocol": "SHANDONG",
  "dataReportEnabled": true
}
```

#### 2.4 更新设备

```http
PUT /api/devices/{deviceCode}
```

**权限**: `ADMIN`

#### 2.5 删除设备

```http
DELETE /api/devices/{deviceCode}
```

**权限**: `ADMIN`

---

### 3. 设备数据接口 (Device Data)

#### 3.1 接收辐射设备数据

```http
POST /api/device-data/radiation
```

**权限**: **公开接口**（无需认证）

**请求头**:
```http
Content-Type: application/json
```

**请求体**:
```json
{
  "deviceCode": "865229085145869",
  "deviceType": "RADIATION_MONITOR",
  "timestamp": "2025-12-30T00:00:00",
  "cpm": 150.5,
  "batvolt": 3950,
  "gps": {
    "longitude": "12102.1465",
    "latitude": "3740.5073",
    "type": "BDS",
    "useful": 1
  }
}
```

**响应**:
```json
{
  "success": true,
  "message": "数据接收成功",
  "dataId": 123
}
```

#### 3.2 接收环境设备数据

```http
POST /api/device-data/environment
```

**权限**: **公开接口**

**请求体**:
```json
{
  "deviceCode": "ENV001",
  "temperature": 25.5,
  "humidity": 60.2,
  "pm25": 35.8
}
```

---

### 4. MQTT数据接收

#### 4.1 MQTT主题订阅

系统自动订阅以下MQTT主题：

| 主题模式 | 说明 | QoS |
|---------|------|-----|
| `ems/device/+/data/RADIATION` | 辐射设备数据 | 1 |
| `ems/device/+/data/ENVIRONMENT` | 环境设备数据 | 1 |

**消息格式** (JSON):
```json
{
  "src": 1,
  "msgtype": 1,
  "BDS": {
    "longitude": "12102.1465",
    "latitude": "3740.5073",
    "useful": 1,
    "UTC": "2025-12-30 00:00:00"
  },
  "CPM": 150,
  "Batvolt": 3950,
  "trigger": 1,
  "multi": 1,
  "way": 1,
  "time": "2025-12-30 00:00:00"
}
```

---

### 5. 实时数据推送 (SSE)

#### 5.1 订阅设备数据

```http
GET /api/sse/subscribe
```

**权限**: `ADMIN`, `USER`

**响应**: `text/event-stream`

**事件格式**:
```
data: {"deviceCode":"865229085145869","cpm":150.5,"batvolt":3950,"timestamp":"2025-12-30T00:00:00"}

data: {"deviceCode":"ENV001","temperature":25.5,"humidity":60.2}
```

---

### 6. 公司管理接口 (Company)

#### 6.1 获取所有公司

```http
GET /api/companies
```

**权限**: `ADMIN`

#### 6.2 创建公司

```http
POST /api/companies
```

**权限**: `ADMIN`

#### 6.3 更新公司

```http
PUT /api/companies/{id}
```

**权限**: `ADMIN`

---

### 7. 用户管理接口 (User)

#### 7.1 获取所有用户

```http
GET /api/users
```

**权限**: `ADMIN`

#### 7.2 创建用户

```http
POST /api/users
```

**权限**: `ADMIN`

**请求体**:
```json
{
  "username": "testuser",
  "password": "password123",
  "email": "test@example.com",
  "role": "USER",
  "companyId": 1
}
```

#### 7.3 修改密码

```http
PUT /api/users/{id}/password
```

**权限**: `ADMIN`, `USER`(仅自己)

---

### 8. 告警接口 (Alert)

#### 8.1 获取告警列表

```http
GET /api/alerts
```

**权限**: `ADMIN`, `USER`

**查询参数**:
- `page`: 页码
- `size`: 每页数量
- `handled`: 是否已处理
- `deviceCode`: 设备编码

**响应**:
```json
{
  "content": [
    {
      "id": 1,
      "deviceCode": "865229085145869",
      "alertType": "HIGH_CPM",
      "alertValue": 150.5,
      "threshold": 100.0,
      "handled": false,
      "alertTime": "2025-12-30T00:00:00"
    }
  ],
  "totalElements": 5
}
```

#### 8.2 处理告警

```http
PUT /api/alerts/{id}/handle
```

**权限**: `ADMIN`, `USER`

---

### 9. 数据上报配置接口 (Data Report Config)

#### 9.1 获取设备上报配置

```http
GET /api/devices/{deviceCode}/report-config
```

**权限**: `ADMIN`, `USER`

**响应**:
```json
{
  "deviceCode": "865229085145869",
  "dataReportEnabled": true,
  "reportProtocol": "SHANDONG",
  "reportUrl": "221.214.62.118:20050",
  "inspectionMachineNumber": "002162",
  "sourceNumber": "DE25IR006722",
  "sourceType": "02"
}
```

#### 9.2 更新上报配置

```http
PUT /api/devices/{deviceCode}/report-config
```

**权限**: `ADMIN`

**请求体**:
```json
{
  "dataReportEnabled": true,
  "reportProtocol": "SHANDONG"
}
```

#### 9.3 获取上报日志

```http
GET /api/devices/{deviceCode}/report-logs
```

**权限**: `ADMIN`, `USER`

**查询参数**:
- `page`: 页码
- `size`: 每页数量
- `status`: 状态（SUCCESS/FAILED）

**响应**:
```json
{
  "content": [
    {
      "id": 1,
      "deviceCode": "865229085145869",
      "reportProtocol": "SHANDONG",
      "reportTime": "2025-12-30T00:00:00",
      "status": "SUCCESS",
      "durationMs": 67,
      "errorMessage": null
    }
  ],
  "totalElements": 4
}
```

---

### 10. 系统接口 (System)

#### 10.1 健康检查

```http
GET /api/actuator/health
```

**权限**: **公开接口**

**响应**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

---

## 🔒 错误码

### HTTP状态码

| 状态码 | 说明 |
|-------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 业务错误码

| 错误码 | 说明 |
|-------|------|
| 1001 | 设备不存在 |
| 1002 | 设备已存在 |
| 1003 | 公司不存在 |
| 1004 | 用户不存在 |
| 1005 | 用户名或密码错误 |
| 2001 | 数据格式错误 |
| 2002 | 设备离线 |

---

## 📝 请求示例

### cURL示例

#### 1. 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

#### 2. 获取设备列表

```bash
curl -X GET http://localhost:8080/api/devices \
  -H "Authorization: Bearer <token>" \
  -G \
  --data-urlencode "deviceType=RADIATION_MONITOR" \
  --data-urlencode "status=ONLINE"
```

#### 3. 接收设备数据

```bash
curl -X POST http://localhost:8080/api/device-data/radiation \
  -H "Content-Type: application/json" \
  -d '{
    "deviceCode": "865229085145869",
    "deviceType": "RADIATION_MONITOR",
    "timestamp": "2025-12-30T00:00:00",
    "cpm": 150.5,
    "batvolt": 3950
  }'
```

### JavaScript示例

```javascript
// 登录
const login = async (username, password) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });
  const data = await response.json();
  localStorage.setItem('token', data.token);
  return data;
};

// 获取设备列表
const getDevices = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/devices', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
};
```

---

## 📚 附录

### 数据类型定义

#### 设备类型 (DeviceType)

| 值 | 说明 |
|-----|------|
| RADIATION_MONITOR | 辐射设备监控器 |
| ENVIRONMENT_STATION | 环境监测站 |

#### 设备状态 (DeviceStatus)

| 值 | 说明 |
|-----|------|
| ONLINE | 在线 |
| OFFLINE | 离线 |
| FAULT | 故障 |
| MAINTENANCE | 维护中 |

#### 用户角色 (UserRole)

| 值 | 说明 |
|-----|------|
| ADMIN | 管理员 |
| USER | 普通用户 |

#### 上报协议 (ReportProtocol)

| 值 | 说明 |
|-----|------|
| SHANDONG | 山东协议（TCP+HJ/T212） |
| SICHUAN | 四川协议（HTTP+SM2） |

---

*文档版本: 1.0*
*最后更新: 2025-12-30*
