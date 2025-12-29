#!/bin/bash
# 测试山东协议 - 通过MQTT发送真实SIM卡号设备数据

SIM_CARD="865229085145869"
BROKER="localhost:1883"
TOPIC="ems/device/${SIM_CARD}/data/RADIATION"

# 生成当前时间字符串
TIME=$(date +"%Y/%m/%d %H:%M:%S")

# 构造设备数据JSON
# 注意：Batvolt单位是毫伏(mV)，CPM是原始值
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

echo "========================================="
echo "山东协议测试 - MQTT设备数据发送"
echo "========================================="
echo "SIM卡号: ${SIM_CARD}"
echo "MQTT Broker: ${BROKER}"
echo "主题: ${TOPIC}"
echo "时间: ${TIME}"
echo ""
echo "发送数据:"
echo "${JSON_DATA}"
echo ""
echo "----------------------------------------"

# 发送MQTT消息
mosquitto_pub -h "${BROKER#tcp://}" -t "${TOPIC}" -m "${JSON_DATA}"

if [ $? -eq 0 ]; then
    echo "✓ MQTT消息发送成功"
    echo ""
    echo "接下来请查看后端日志:"
    echo "  1. MQTT消息接收日志"
    echo "  2. DeviceDataEvent发布日志"
    echo "  3. 山东协议上报日志"
    echo "  4. CM消息处理日志"
    echo "  5. 服务器响应状态码"
else
    echo "✗ MQTT消息发送失败"
    echo "请检查Mosquitto是否运行: docker ps | grep mosquitto"
fi
echo "========================================="
