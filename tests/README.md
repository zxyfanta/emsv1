# 测试脚本目录

本目录包含各类测试脚本，用于验证系统功能。

## 目录结构

```
tests/
├── protocol/          # 协议测试脚本
│   ├── test_final.py
│   ├── test_handshake_analysis.py
│   ├── test_java_format.py
│   ├── test_shandong_handshake.py
│   └── test_shandong_quick.py
├── mqtt/             # MQTT测试脚本
│   ├── test_mqtt_sim_device.sh
│   └── test_shandong_protocol.sh
├── sql/              # 数据库脚本
│   └── create_shandong_test_device.sql
└── 测试脚本使用说明.md
```

## 协议测试

### 山东协议测试

**test_shandong_quick.py** - 快速测试脚本
- 测试TCP连接
- 发送测试数据包
- 验证服务器响应

**test_final.py** - 完整测试脚本
- CM握手流程测试
- 数据上报测试
- 服务器响应验证

**test_java_format.py** - Java格式测试
- 与Java代码格式对比
- 数据包构建验证

### 使用方法

```bash
# 运行快速测试
python3 tests/protocol/test_shandong_quick.py

# 运行完整测试
python3 tests/protocol/test_final.py

# 分析握手流程
python3 tests/protocol/test_handshake_analysis.py
```

## MQTT测试

### 测试脚本

**test_mqtt_sim_device.sh** - 模拟设备数据发送
- 发送MQTT测试消息
- 模拟设备数据上报

**test_shandong_protocol.sh** - 山东协议完整流程
- MQTT数据发送
- 数据库验证
- 上报流程测试

### 使用方法

```bash
# 运行MQTT模拟设备测试
./tests/mqtt/test_mqtt_sim_device.sh

# 运行山东协议完整测试
./tests/mqtt/test_shandong_protocol.sh
```

## 数据库脚本

### create_shandong_test_device.sql

创建山东协议测试设备：
- 设备编码: 865229085145869
- 上报协议: SHANDONG
- 数据上报: 已启用

### 使用方法

```bash
# 连接到MySQL并执行脚本
mysql -u ems_user -pems_password ems_db < tests/sql/create_shandong_test_device.sql
```

## 测试说明

详细的测试脚本使用说明请参考: [测试脚本使用说明.md](./测试脚本使用说明.md)

---

最后更新: 2025-12-30
