package com.cdutetc.ems.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数据上报异步配置
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class DataReportAsyncConfig {

    private final DataReportProperties dataReportProperties;

    /**
     * 数据上报专用线程池
     */
    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 配置线程池参数
        executor.setCorePoolSize(dataReportProperties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(dataReportProperties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(dataReportProperties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix(dataReportProperties.getAsync().getThreadNamePrefix());

        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
