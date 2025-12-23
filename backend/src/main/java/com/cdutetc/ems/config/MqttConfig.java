package com.cdutetc.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT配置属性类
 * 绑定application.yaml中的ems.mqtt配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ems.mqtt")
public class MqttConfig {

    /**
     * MQTT服务器地址
     */
    private String host = "localhost";

    /**
     * MQTT服务器端口
     */
    private int port = 1883;

    /**
     * MQTT客户端ID
     */
    private String clientId = "ems-backend-server";

    /**
     * MQTT用户名（可选）
     */
    private String username = "";

    /**
     * MQTT密码（可选）
     */
    private String password = "";

    /**
     * MQTT主题前缀
     */
    private String topicPrefix = "ems";

    /**
     * 服务质量等级
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
     * 自动注册设备的默认公司ID
     */
    private Long defaultCompanyId = 1L;

    /**
     * 获取完整的MQTT Broker URL
     */
    public String getBrokerUrl() {
        return String.format("tcp://%s:%d", host, port);
    }

    /**
     * 获取唯一的客户端ID（添加时间戳避免冲突）
     */
    public String getClientId() {
        return clientId + "-" + System.currentTimeMillis();
    }

    /**
     * 获取辐射设备数据主题模式
     */
    public String getRadiationTopicPattern() {
        return topicPrefix + "/device/+/data/RADIATION";
    }

    /**
     * 获取环境设备数据主题模式
     */
    public String getEnvironmentTopicPattern() {
        return topicPrefix + "/device/+/data/ENVIRONMENT";
    }

    /**
     * 获取所有设备数据主题数组
     */
    public String[] getSubscribeTopics() {
        return new String[]{
            getRadiationTopicPattern(),
            getEnvironmentTopicPattern()
        };
    }

    /**
     * 获取订阅主题对应的QoS数组
     */
    public int[] getSubscribeQos() {
        return new int[]{qos, qos};
    }
}