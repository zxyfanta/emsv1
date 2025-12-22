package com.cdutetc.ems.test;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import lombok.extern.slf4j.Slf4j;

/**
 * MQTT测试客户端
 * 用于测试EMS后端MQTT集成功能
 */
@Slf4j
public class MqttTestClient {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String CLIENT_ID = "ems-test-client";
    private static final String RADIATION_TOPIC = "ems/device/TEST001/data/radiation";
    private static final String ENVIRONMENT_TOPIC = "ems/device/TEST002/data/environment";

    public static void main(String[] args) {
        try {
            // 创建MQTT客户端
            MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(false);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);

            // 连接到MQTT Broker
            log.info("连接到MQTT Broker: {}", BROKER_URL);
            client.connect(options);
            log.info("MQTT连接成功!");

            // 发送辐射设备测试数据
            sendRadiationTestData(client);

            // 等待1秒
            Thread.sleep(1000);

            // 发送环境设备测试数据
            sendEnvironmentTestData(client);

            // 等待1秒
            Thread.sleep(1000);

            // 发送未知设备测试数据（测试自动注册）
            sendUnknownDeviceTestData(client);

            // 等待2秒让消息处理完成
            Thread.sleep(2000);

            // 断开连接
            client.disconnect();
            client.close();
            log.info("MQTT测试完成!");

        } catch (Exception e) {
            log.error("MQTT测试失败", e);
        }
    }

    /**
     * 发送辐射设备测试数据
     */
    private static void sendRadiationTestData(MqttClient client) throws MqttException {
        String radiationData = "{"
                + "\"deviceCode\":\"TEST001\","
                + "\"deviceType\":\"radiation\","
                + "\"src\":1,"
                + "\"cpm\":15.5,"
                + "\"batvolt\":3.7,"
                + "\"msgtype\":1,"
                + "\"trigger\":1,"
                + "\"multi\":1,"
                + "\"way\":1,"
                + "\"time\":\"2025-12-23T00:25:00\","
                + "\"bdsLongitude\":\"116.404\","
                + "\"bdsLatitude\":\"39.915\","
                + "\"bdsUtc\":\"2025-12-23T00:25:00\","
                + "\"rawData\":\"RADIATION_TEST_DATA\""
                + "}";

        MqttMessage radiationMessage = new MqttMessage(radiationData.getBytes());
        radiationMessage.setQos(1);
        radiationMessage.setRetained(false);

        log.info("发送辐射设备测试数据到主题: {}", RADIATION_TOPIC);
        client.publish(RADIATION_TOPIC, radiationMessage);
        log.info("辐射设备测试数据发送成功!");
    }

    /**
     * 发送环境设备测试数据
     */
    private static void sendEnvironmentTestData(MqttClient client) throws MqttException {
        String environmentData = "{"
                + "\"deviceCode\":\"TEST002\","
                + "\"deviceType\":\"environment\","
                + "\"src\":2,"
                + "\"cpm\":12.3,"
                + "\"temperature\":25.6,"
                + "\"wetness\":60.8,"
                + "\"windspeed\":3.2,"
                + "\"total\":100.0,"
                + "\"battery\":85.5,"
                + "\"time\":\"2025-12-23T00:25:00\","
                + "\"rawData\":\"ENVIRONMENT_TEST_DATA\""
                + "}";

        MqttMessage environmentMessage = new MqttMessage(environmentData.getBytes());
        environmentMessage.setQos(1);
        environmentMessage.setRetained(false);

        log.info("发送环境设备测试数据到主题: {}", ENVIRONMENT_TOPIC);
        client.publish(ENVIRONMENT_TOPIC, environmentMessage);
        log.info("环境设备测试数据发送成功!");
    }

    /**
     * 发送未知设备测试数据（测试自动注册功能）
     */
    private static void sendUnknownDeviceTestData(MqttClient client) throws MqttException {
        String unknownDeviceData = "{"
                + "\"deviceCode\":\"UNKNOWN003\","
                + "\"deviceType\":\"radiation\","
                + "\"src\":3,"
                + "\"cpm\":22.1,"
                + "\"batvolt\":3.6,"
                + "\"msgtype\":1,"
                + "\"trigger\":1,"
                + "\"multi\":1,"
                + "\"way\":1,"
                + "\"time\":\"2025-12-23T00:25:00\","
                + "\"rawData\":\"UNKNOWN_DEVICE_TEST_DATA\""
                + "}";

        String unknownDeviceTopic = "ems/device/UNKNOWN003/data/radiation";
        MqttMessage unknownDeviceMessage = new MqttMessage(unknownDeviceData.getBytes());
        unknownDeviceMessage.setQos(1);
        unknownDeviceMessage.setRetained(false);

        log.info("发送未知设备测试数据到主题: {}", unknownDeviceTopic);
        client.publish(unknownDeviceTopic, unknownDeviceMessage);
        log.info("未知设备测试数据发送成功!");
    }
}