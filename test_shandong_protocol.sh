#!/bin/bash
# ==========================================
# 山东协议完整测试流程
# SIM卡号: 865229085145869
# ==========================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
SIM_CARD="865229085145869"
MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_USER="ems_user"
MYSQL_PASS="ems_pass"
MYSQL_DB="ems_db"
MQTT_BROKER="localhost:1883"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}山东协议完整测试流程${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ==========================================
# 步骤1: 检查Docker服务状态
# ==========================================
echo -e "${YELLOW}[步骤1] 检查Docker服务状态...${NC}"

# 检查MySQL
if docker ps | grep -q "ems-mysql"; then
    echo -e "${GREEN}✓ MySQL运行中${NC}"
else
    echo -e "${RED}✗ MySQL未运行，正在启动...${NC}"
    docker-compose up -d mysql
    sleep 10  # 等待MySQL启动
fi

# 检查Redis
if docker ps | grep -q "ems-redis"; then
    echo -e "${GREEN}✓ Redis运行中${NC}"
else
    echo -e "${RED}✗ Redis未运行，正在启动...${NC}"
    docker-compose up -d redis
    sleep 5
fi

# 检查Mosquitto
if docker ps | grep -q "ems-mosquitto"; then
    echo -e "${GREEN}✓ Mosquitto运行中${NC}"
else
    echo -e "${RED}✗ Mosquitto未运行，正在启动...${NC}"
    docker-compose up -d mosquitto
    sleep 5
fi

echo ""

# ==========================================
# 步骤2: 执行SQL创建测试设备
# ==========================================
echo -e "${YELLOW}[步骤2] 创建测试设备...${NC}"

# 检查设备是否已存在
DEVICE_EXISTS=$(docker exec ems-mysql mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASS}" "${MYSQL_DB}" -se "SELECT COUNT(*) FROM ems_device WHERE device_code='${SIM_CARD}'" 2>/dev/null || echo "0")

if [ "$DEVICE_EXISTS" -gt 0 ]; then
    echo -e "${YELLOW}! 设备已存在，跳过创建${NC}"
else
    echo "正在创建设备..."
    docker exec -i ems-mysql mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASS}" "${MYSQL_DB}" < create_shandong_test_device.sql

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 设备创建成功${NC}"
    else
        echo -e "${RED}✗ 设备创建失败${NC}"
        exit 1
    fi
fi

echo ""

# ==========================================
# 步骤3: 显示设备信息
# ==========================================
echo -e "${YELLOW}[步骤3] 验证设备配置...${NC}"

docker exec ems-mysql mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASS}" "${MYSQL_DB}" -e "
SELECT
    CONCAT('SIM卡号: ', device_code) AS ''
FROM ems_device WHERE device_code='${SIM_CARD}';
SELECT
    CONCAT('探伤机编号: ', inspection_machine_number) AS ''
FROM ems_device WHERE device_code='${SIM_CARD}';
SELECT
    CONCAT('放射源编号: ', source_number) AS ''
FROM ems_device WHERE device_code='${SIM_CARD}';
SELECT
    CONCAT('上报协议: ', report_protocol) AS ''
FROM ems_device WHERE device_code='${SIM_CARD}';
SELECT
    CONCAT('激活状态: ', activation_status) AS ''
FROM ems_device WHERE device_code='${SIM_CARD}';
" 2>/dev/null

echo ""

# ==========================================
# 步骤4: 生成并发送MQTT测试数据
# ==========================================
echo -e "${YELLOW}[步骤4] 发送MQTT测试数据...${NC}"

# 生成当前时间字符串
TIME=$(date +"%Y/%m/%d %H:%M:%S")

# 构造设备数据JSON
JSON_DATA=$(cat <<EOF
{
  "src": 1,
  "msgtype": 1,
  "BDS": {
    "longitude": "12102.1465",
    "latitude": "3740.5073",
    "useful": 1,
    "UTC": "${TIME}"
  },
  "CPM": 45,
  "Batvolt": 3989,
  "trigger": 1,
  "multi": 1,
  "way": 1,
  "time": "${TIME}"
}
EOF
)

TOPIC="ems/device/${SIM_CARD}/data/RADIATION"

echo "SIM卡号: ${SIM_CARD}"
echo "主题: ${TOPIC}"
echo "时间: ${TIME}"
echo ""
echo "发送数据:"
echo "${JSON_DATA}"
echo ""

# 发送MQTT消息
if command -v mosquitto_pub &> /dev/null; then
    mosquitto_pub -h "${MQTT_BROKER#tcp://}" -t "${TOPIC}" -m "${JSON_DATA}"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ MQTT消息发送成功${NC}"
    else
        echo -e "${RED}✗ MQTT消息发送失败${NC}"
        echo "请检查Mosquitto是否运行: docker ps | grep mosquitto"
    fi
else
    echo -e "${RED}✗ mosquitto_pub命令未找到${NC}"
    echo "请安装: brew install mosquitto (macOS) 或 apt install mosquitto-clients (Ubuntu)"
fi

echo ""

# ==========================================
# 步骤5: 提示查看后端日志
# ==========================================
echo -e "${YELLOW}[步骤5] 查看后端日志验证结果...${NC}"
echo ""
echo -e "${BLUE}预期日志流程:${NC}"
echo "1. 📥 收到MQTT消息 - 主题: ems/device/${SIM_CARD}/data/RADIATION"
echo "2. 📤 DeviceDataEvent发布成功: ${SIM_CARD}"
echo "3. 📤 [山东] 开始上报: deviceCode=${SIM_CARD}"
echo "4. 🔌 TCP连接已建立: 221.214.62.118:20050"
echo "5. 📥 [山东] 收到服务器初始消息: 9 字节"
echo "6. ℹ️ [山东] 策略: 忽略初始消息，不响应握手"
echo "7. 📤 数据已发送"
echo "8. 📥 [山东] 服务器响应: hex=43 4D 01..."
echo "9. ✅ [山东] 上报成功 (状态码0x01)"
echo ""
echo -e "${YELLOW}如果后端已在运行，请查看控制台日志${NC}"
echo -e "${YELLOW}如果后端未运行，请执行: cd backend && mvn spring-boot:run${NC}"
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}测试完成！${NC}"
echo -e "${BLUE}========================================${NC}"
