package com.cdutetc.ems.config;

import com.cdutetc.ems.mqtt.MqttMessageListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MQTT客户端配置类
 * 负责MQTT连接的初始化、管理和维护
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttClientConfig implements SmartLifecycle {

    private final MqttConfig mqttConfig;
    private final MqttMessageListener messageListener;

    private MqttClient mqttClient;
    private volatile boolean isRunning = false;
    private volatile boolean isConnected = false;

    // 重连配置
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int INITIAL_DELAY_SECONDS = 30; // 延迟30秒启动，确保Spring完全启动
    private static final int RETRY_DELAY_SECONDS = 10;

    /**
     * 应用启动完成后安排MQTT客户端初始化
     */
    @PostConstruct
    public void scheduleInitialization() {
        log.info("🚀 MQTT客户端配置完成，{}秒后将初始化连接...", INITIAL_DELAY_SECONDS);
        CompletableFuture.delayedExecutor(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .execute(this::initializeMqttClientWithRetry);
    }

    /**
     * 带重试机制的MQTT连接方法
     */
    private void initializeMqttClientWithRetry() {
        int retryAttempt = 0;

        while (retryAttempt < MAX_RETRY_ATTEMPTS && !isConnected) {
            retryAttempt++;
            log.info("🔗 尝试MQTT连接 (第{}/{}次)", retryAttempt, MAX_RETRY_ATTEMPTS);

            try {
                initializeMqttClient();
                if (isConnected) {
                    log.info("✅ MQTT客户端连接成功！");
                    return;
                }
            } catch (MqttException e) {
                log.warn("⚠️ MQTT连接失败 (第{}次尝试): {}", retryAttempt, e.getMessage());
            }

            if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("❌ MQTT连接重试次数已达上限({}次)，停止重试", MAX_RETRY_ATTEMPTS);
        schedulePeriodicConnectionCheck();
    }

    /**
     * 初始化MQTT客户端
     */
    private void initializeMqttClient() throws MqttException {
        log.debug("🔧 开始初始化MQTT客户端...");

        MqttConnectOptions options = createConnectionOptions();
        String clientId = mqttConfig.getClientId();

        // 创建MQTT客户端
        mqttClient = new MqttClient(mqttConfig.getBrokerUrl(), clientId, new MemoryPersistence());
        mqttClient.setCallback(messageListener);

        // 连接到MQTT Broker
        mqttClient.connect(options);
        isConnected = mqttClient.isConnected();

        if (isConnected) {
            // 订阅主题
            subscribeToTopics();
            log.info("🎯 MQTT连接状态验证通过，开始接收消息");
        } else {
            log.warn("⚠️ MQTT连接状态异常，将在下次检查时重试");
        }
    }

    /**
     * 创建MQTT连接选项
     */
    private MqttConnectOptions createConnectionOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{mqttConfig.getBrokerUrl()});
        options.setCleanSession(mqttConfig.isCleanSession());
        options.setAutomaticReconnect(mqttConfig.isAutoReconnect());
        options.setConnectionTimeout(mqttConfig.getConnectionTimeout());
        options.setKeepAliveInterval(mqttConfig.getKeepAliveInterval());
        options.setMaxReconnectDelay(30000); // 最大重连延迟30秒
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        // 设置认证信息
        if (mqttConfig.getUsername() != null && !mqttConfig.getUsername().isEmpty()) {
            options.setUserName(mqttConfig.getUsername());
            options.setPassword(mqttConfig.getPassword().toCharArray());
        }

        log.debug("🔧 MQTT连接选项: Broker={}, Username={}, CleanSession={}, AutoReconnect={}",
                mqttConfig.getBrokerUrl(), mqttConfig.getUsername(),
                mqttConfig.isCleanSession(), mqttConfig.isAutoReconnect());

        return options;
    }

    /**
     * 订阅设备数据主题
     */
    private void subscribeToTopics() {
        try {
            String[] topics = mqttConfig.getSubscribeTopics();
            int[] qos = mqttConfig.getSubscribeQos();

            mqttClient.subscribe(topics, qos);

            log.info("📡 成功订阅MQTT主题:");
            for (int i = 0; i < topics.length; i++) {
                log.info("  - {} (QoS: {})", topics[i], qos[i]);
            }
        } catch (MqttException e) {
            log.error("❌ 订阅MQTT主题失败", e);
            throw new RuntimeException("MQTT主题订阅失败", e);
        }
    }

    /**
     * 定期检查MQTT连接状态
     */
    private void schedulePeriodicConnectionCheck() {
        CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(() -> {
            try {
                checkMqttConnection();
                // 递归安排下次检查
                if (isRunning && !isConnected) {
                    schedulePeriodicConnectionCheck();
                }
            } catch (Exception e) {
                log.error("定期连接检查出错", e);
                if (isRunning && !isConnected) {
                    schedulePeriodicConnectionCheck();
                }
            }
        });
    }

    /**
     * 检查MQTT连接状态
     */
    public void checkMqttConnection() {
        try {
            log.debug("🔍 检查MQTT连接状态...");

            if (mqttClient == null || !mqttClient.isConnected()) {
                log.warn("🔍 检测到MQTT连接断开，尝试重新连接");
                isConnected = false;
                initializeMqttClientWithRetry();
            } else {
                isConnected = true;
                log.debug("✅ MQTT连接状态正常");
            }
        } catch (Exception e) {
            log.error("❌ MQTT连接状态检查失败", e);
            isConnected = false;
        }
    }

    /**
     * 处理连接失败
     */
    private void handleConnectionFailure(MqttException e) {
        isConnected = false;
        log.error("❌ MQTT连接失败: {}", e.getMessage(), e);

        // 根据异常类型决定处理策略
        if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_DISCONNECTING) {
            log.info("👋 MQTT客户端正在断开连接");
        } else if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_TIMEOUT) {
            log.warn("⏰ MQTT连接超时，将尝试重连");
            schedulePeriodicConnectionCheck();
        } else {
            log.error("❌ MQTT连接发生未知错误: {}", e.getMessage());
            schedulePeriodicConnectionCheck();
        }
    }

    // SmartLifecycle 接口实现

    @Override
    public void start() {
        log.info("🚀 启动MQTT客户端配置");
        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("🛑 停止MQTT客户端配置");
        isRunning = false;

        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("👋 MQTT客户端已断开连接");
            } catch (MqttException e) {
                log.error("❌ 断开MQTT连接失败", e);
            }
        }

        isConnected = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        // 返回较高的阶段值，确保在其他组件之后启动
        return 1000;
    }
}