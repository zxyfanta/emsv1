package com.ems.config;

import com.ems.service.mqtt.MqttMessageReceiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MQTT客户端配置（支持延迟连接和重试机制）
 * 配置MQTT连接和消息监听
 *
 * 改进特性：
 * - 延迟初始化：等待EMQX认证器完全就绪
 * - 指数退避重试：智能重试策略
 * - 连接状态监控：定期检查和自动恢复
 *
 * @author EMS Team
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MqttClientConfig {

    private final MqttConfig mqttConfig;
    private final MqttMessageReceiver mqttMessageReceiver;
    private MqttClient mqttClient;

    // 重试机制配置
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final int INITIAL_DELAY_SECONDS = 30;
    private static final int MAX_DELAY_SECONDS = 300; // 5分钟
    private static final int CONNECTION_CHECK_INTERVAL_SECONDS = 60;

    // 连接状态管理
    private int retryAttempt = 0;
    private boolean isConnecting = false;
    private boolean connectionInitialized = false;

    /**
     * 应用启动完成后安排MQTT客户端初始化
     * 延迟30秒等待EMQX认证器完全就绪
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleMqttInitialization() {
        log.info("🚀 应用启动完成，{}秒后将初始化MQTT客户端...", INITIAL_DELAY_SECONDS);

        // 异步延迟初始化，不阻塞应用启动
        CompletableFuture.delayedExecutor(INITIAL_DELAY_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> {
                    log.info("⏰ 开始初始化MQTT客户端");
                    connectWithRetry();
                });
    }

    /**
     * 带重试机制的MQTT连接方法
     */
    private void connectWithRetry() {
        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            log.error("❌ MQTT连接重试次数已达上限({}次)，停止重试", MAX_RETRY_ATTEMPTS);
            return;
        }

        if (isConnecting) {
            log.debug("🔄 MQTT连接正在进行中，跳过本次重试");
            return;
        }

        isConnecting = true;
        retryAttempt++;

        try {
            log.info("🔗 尝试MQTT连接 (第{}/{}次)", retryAttempt, MAX_RETRY_ATTEMPTS);

            // 创建连接选项
            MqttConnectOptions options = createConnectionOptions();

            // 创建MQTT客户端
            String clientId = mqttConfig.getClientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("📋 使用客户端ID: {}", clientId);

            mqttClient = new MqttClient(mqttConfig.getBrokerUrl(), clientId, new MemoryPersistence());
            mqttClient.setCallback(mqttMessageReceiver);

            // 尝试连接
            mqttClient.connect(options);

            // 连接成功处理
            handleConnectionSuccess();

        } catch (MqttException e) {
            handleConnectionFailure(e);
        } finally {
            isConnecting = false;
        }
    }

    /**
     * 创建MQTT连接选项
     */
    private MqttConnectOptions createConnectionOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{mqttConfig.getBrokerUrl()});
        options.setUserName(mqttConfig.getUsername());
        options.setPassword(mqttConfig.getPassword().toCharArray());
        options.setCleanSession(mqttConfig.isCleanSession());
        options.setAutomaticReconnect(mqttConfig.isAutoReconnect());
        options.setConnectionTimeout(mqttConfig.getConnectionTimeout());
        options.setKeepAliveInterval(mqttConfig.getKeepAliveInterval());

        // 设置最大重连间隔
        options.setMaxReconnectDelay(MAX_DELAY_SECONDS * 1000);

        log.debug("🔧 MQTT连接选项: Broker={}, Username={}, CleanSession={}, AutoReconnect={}",
                 mqttConfig.getBrokerUrl(), mqttConfig.getUsername(),
                 mqttConfig.isCleanSession(), mqttConfig.isAutoReconnect());

        return options;
    }

    /**
     * 处理连接成功
     */
    private void handleConnectionSuccess() {
        retryAttempt = 0; // 重置重试计数
        connectionInitialized = true;

        log.info("✅ MQTT客户端连接成功!");

        try {
            // 订阅主题
            subscribeToTopics();

            // 验证连接状态
            if (mqttClient != null && mqttClient.isConnected()) {
                log.info("🎯 MQTT连接状态验证通过，开始接收消息");
            } else {
                log.warn("⚠️ MQTT连接状态异常，将在下次检查时重试");
            }

        } catch (Exception e) {
            log.error("❌ 订阅MQTT主题失败", e);
            // 即使订阅失败，也不断开连接，让定时任务处理
        }
    }

    /**
     * 处理连接失败
     */
    private void handleConnectionFailure(MqttException e) {
        log.warn("⚠️ MQTT连接失败 (第{}次尝试): {}", retryAttempt, e.getMessage());

        if (retryAttempt < MAX_RETRY_ATTEMPTS) {
            int delaySeconds = calculateBackoffDelay(retryAttempt);
            log.info("🔄 {}秒后将进行第{}次重试", delaySeconds, retryAttempt + 1);

            // 异步安排下次重试
            CompletableFuture.delayedExecutor(delaySeconds, java.util.concurrent.TimeUnit.SECONDS)
                    .execute(this::connectWithRetry);
        } else {
            log.error("❌ MQTT连接重试次数用尽，启用定时检查模式");
        }
    }

    /**
     * 计算指数退避延迟时间
     */
    private int calculateBackoffDelay(int attempt) {
        // 指数退避: 2^(attempt-1) * 初始间隔，最大不超过MAX_DELAY_SECONDS
        int delay = INITIAL_DELAY_SECONDS * (int) Math.pow(2, attempt - 1);
        return Math.min(delay, MAX_DELAY_SECONDS);
    }

    /**
     * 定期检查MQTT连接状态
     * 每60秒检查一次，如果连接断开则尝试重连
     */
    @Scheduled(fixedDelay = CONNECTION_CHECK_INTERVAL_SECONDS * 1000)
    public void checkMqttConnection() {
        // 如果从未初始化过连接，跳过检查
        if (!connectionInitialized) {
            return;
        }

        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                log.warn("🔍 检测到MQTT连接断开，尝试重新连接");

                if (!isConnecting) {
                    connectWithRetry();
                } else {
                    log.debug("🔄 重连正在进行中，跳过本次检查");
                }
            } else {
                log.debug("✅ MQTT连接状态正常");
            }
        } catch (Exception e) {
            log.error("❌ 检查MQTT连接状态时出错", e);
        }
    }

    /**
     * 订阅MQTT主题
     */
    private void subscribeToTopics() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            log.info("📡 开始订阅MQTT主题...");

            // 订阅设备数据主题
            mqttClient.subscribe(mqttConfig.getDeviceDataTopic(), mqttConfig.getQos());
            log.info("📋 已订阅设备数据主题: {} (QoS: {})", mqttConfig.getDeviceDataTopic(), mqttConfig.getQos());

            // 订阅设备状态主题
            mqttClient.subscribe(mqttConfig.getDeviceStatusTopic(), mqttConfig.getQos());
            log.info("📋 已订阅设备状态主题: {} (QoS: {})", mqttConfig.getDeviceStatusTopic(), mqttConfig.getQos());

            // 订阅设备告警主题
            mqttClient.subscribe(mqttConfig.getDeviceAlertTopic(), 2);
            log.info("📋 已订阅设备告警主题: {} (QoS: 2)", mqttConfig.getDeviceAlertTopic());

            log.info("🎯 所有MQTT主题订阅完成");
        } else {
            log.warn("⚠️ MQTT客户端未连接，跳过主题订阅");
        }
    }

    /**
     * 获取当前连接状态
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 获取连接状态详情
     */
    public String getConnectionStatus() {
        if (!connectionInitialized) {
            return "未初始化";
        } else if (isConnecting) {
            return String.format("连接中 (第%d次尝试)", retryAttempt);
        } else if (isConnected()) {
            return "已连接";
        } else {
            return String.format("连接断开 (已重试%d次/%d次)", retryAttempt, MAX_RETRY_ATTEMPTS);
        }
    }

    /**
     * 应用关闭时断开MQTT连接
     */
    @PreDestroy
    public void disconnect() {
        try {
            connectionInitialized = false;

            if (mqttClient != null) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    log.info("🔌 MQTT客户端已断开连接");
                }
                mqttClient.close();
                log.info("🗑️ MQTT客户端资源已清理");
            }
        } catch (MqttException e) {
            log.error("❌ 断开MQTT连接时出错: {}", e.getMessage(), e);
        }
    }
}