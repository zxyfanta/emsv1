package com.cdutetc.ems.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据上报配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.data-report")
public class DataReportProperties {

    /**
     * 四川上报配置
     */
    private Sichuan sichuan = new Sichuan();

    /**
     * 山东上报配置
     */
    private Shandong shandong = new Shandong();

    /**
     * 异步上报配置
     */
    private Async async = new Async();

    /**
     * 四川上报配置
     */
    @Data
    public static class Sichuan {
        /**
         * 上报地址
         */
        private String url = "http://59.225.208.12:18085/access/data/report";

        /**
         * API密钥
         */
        private String apiKey = "AK-9lKIK7RZrHxPsxq9eYzb2BWBCq8WeSwIPC2e";

        /**
         * 是否启用四川上报
         */
        private boolean enabled = true;

        /**
         * SM2公钥（用于加密）
         */
        private String sm2PublicKey;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 30000;

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 重试间隔（毫秒）
         */
        private long retryInterval = 5000;
    }

    /**
     * 山东上报配置
     */
    @Data
    public static class Shandong {
        /**
         * TCP服务器地址
         */
        private String host = "117.73.252.128";

        /**
         * TCP服务器端口
         */
        private int port = 8091;

        /**
         * 是否启用山东上报
         */
        private boolean enabled = true;

        /**
         * 访问密码
         */
        private String password = "123456";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * socket超时时间（毫秒）
         */
        private int soTimeout = 30000;

        /**
         * 心跳间隔（毫秒）
         */
        private long heartbeatInterval = 60000;

        /**
         * 是否自动重连
         */
        private boolean autoReconnect = true;

        /**
         * 重连间隔（毫秒）
         */
        private long reconnectInterval = 10000;
    }

    /**
     * 异步上报配置
     */
    @Data
    public static class Async {
        /**
         * 核心线程池大小
         */
        private int corePoolSize = 5;

        /**
         * 最大线程池大小
         */
        private int maxPoolSize = 10;

        /**
         * 队列容量
         */
        private int queueCapacity = 100;

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "data-report-";
    }
}
