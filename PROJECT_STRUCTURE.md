# EMS 项目目录结构

## 核心目录

```
ems/
├── backend/           # Spring Boot后端服务
├── frontend/          # Vue前端应用
├── nodered/           # Node-RED设备模拟器
├── mosquitto-config/  # MQTT Broker配置
├── docs/              # 项目文档
├── tests/             # 测试脚本
└── docker-compose.yml # Docker编排配置
```

## 文档目录 (docs/)

```
docs/
├── CLAUDE.md          # Claude Code项目指南
├── README.md          # 文档目录说明
├── design/           # 设计文档（12个）
│   ├── ALERT_*.md               # 告警系统设计
│   ├── CPM转换系数配置说明.md
│   ├── GPS数据处理优化方案.md
│   └── *上报*.md                # 数据上报设计
└── reports/          # 报告文档（16个）
    ├── *_SUMMARY.md              # 实现总结
    ├── *_REPORT.md              # 测试报告
    ├── 协议*.md                 # 协议分析
    └── 测试*.md                 # 测试文档
```

## 测试目录 (tests/)

```
tests/
├── README.md          # 测试说明
├── protocol/         # 协议测试（5个Python脚本）
│   ├── test_final.py
│   ├── test_shandong_quick.py
│   ├── test_handshake_analysis.py
│   ├── test_java_format.py
│   └── test_shandong_handshake.py
├── mqtt/             # MQTT测试（3个Shell脚本）
│   ├── test_mqtt_sim_device.sh
│   ├── test_shandong_protocol.sh
│   └── start-docker-test.bat
└── sql/              # SQL脚本（1个）
    └── create_shandong_test_device.sql
```

## 配置文件

```
├── .gitignore         # Git忽略规则（已更新）
├── CLAUDE.md          # 保留在根目录
├── docker-compose.yml # Docker服务编排
└── README.md          # 项目说明
```

## 已清理

- ✅ 临时测试脚本已分类到 tests/
- ✅ 文档已分类到 docs/
- ✅ .gitignore已更新（Python/Node.js/日志）
- ✅ Playwright临时截图已删除
- ✅ 临时日志文件已清理

## 下一步

如需推送到远程仓库：
```bash
git push origin master
```
