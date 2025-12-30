package com.cdutetc.ems.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置类
 *
 * 功能:
 * 1. 启用Spring异步任务支持(@EnableAsync)
 * 2. 配置缓存同步专用线程池
 * 3. 配置异步异常处理器
 *
 * @author EMS Team
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 缓存同步专用线程池
     *
     * 用途: 执行设备缓存延迟双删任务
     * - 核心线程数: 5
     * - 最大线程数: 10
     * - 队列容量: 100
     * - 拒绝策略: CallerRunsPolicy(降级为同步执行)
     *
     * @return 缓存同步线程池执行器
     */
    @Bean(name = "cacheSyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程池大小
        executor.setCorePoolSize(5);

        // 最大线程池大小
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名称前缀
        executor.setThreadNamePrefix("cache-sync-");

        // 拒绝策略: 由调用线程执行（降级为同步执行）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("缓存同步线程池初始化完成: corePoolSize=5, maxSize=10, queueCapacity=100");

        return executor;
    }

    /**
     * 异步任务异常处理器
     *
     * @return 异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步任务执行异常 - 方法: {}, 参数: {}, 异常: {}",
                method.getName(), params, throwable.getMessage(), throwable);
        };
    }
}
