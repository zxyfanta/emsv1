package com.ems.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT客户端配置类
 *
 * @author EMS Team
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ems.mqtt")
@Data
public class MqttConfig {

    /**
     * MQTT Broker地址
     */
    private String host = System.getenv().getOrDefault("EMS_MQTT_HOST", "localhost");

    /**
     * MQTT Broker端口
     */
    private int port = Integer.parseInt(System.getenv().getOrDefault("EMS_MQTT_PORT", "1883"));

    /**
     * 客户端ID
     */
    private String clientId = "ems-backend-server";

    /**
     * 用户名
     */
    private String username = System.getenv().getOrDefault("EMS_MQTT_USERNAME", "");

    /**
     * 密码
     */
    private String password = System.getenv().getOrDefault("EMS_MQTT_PASSWORD", "");

    /**
     * 主题前缀
     */
    private String topicPrefix = "ems";

    /**
     * QoS等级
     */
    private int qos = 1;

    /**
     * 是否清除会话
     */
    private boolean cleanSession = true;

    /**
     * 是否自动重连
     */
    private boolean autoReconnect = true;

    /**
     * 连接超时时间（秒）
     */
    private int connectionTimeout = 30;

    /**
     * 心跳间隔（秒）
     */
    private int keepAliveInterval = 60;

    /**
     * 获取完整的MQTT Broker地址
     */
    public String getBrokerUrl() {
        return String.format("tcp://%s:%d", host, port);
    }

      /**
     * 获取设备数据主题 - 支持设备类型子路径
     */
    public String getDeviceDataTopic() {
        return String.format("%s/device/+/data/+", topicPrefix);
    }

    /**
     * 获取GPS设备数据主题
     */
    public String getGpsDataTopic() {
        return String.format("%s/device/+/data", topicPrefix); // 复用同一个主题
    }

    /**
     * 获取设备状态主题
     */
    public String getDeviceStatusTopic() {
        return String.format("%s/device/+/status", topicPrefix);
    }

    /**
     * 获取设备告警主题
     */
    public String getDeviceAlertTopic() {
        return String.format("%s/device/+/alert", topicPrefix);
    }
}