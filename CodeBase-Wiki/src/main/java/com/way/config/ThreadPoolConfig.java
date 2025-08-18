package com.way.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author way
 * @description: 线程池配置
 * @date 2025/7/20 15:46
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {
    /**
     * 核心线程数：操作系统线程数+2
     */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 2;
    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    /**
     * 队列长度
     */
    private static final int QUEUE_CAPACITY = 10000;

    @Bean(name = "CreateTaskExecutor")
    public ThreadPoolTaskExecutor createTaskExecutor(MdcTaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("CreateTaskExecutor-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean(name="GenCatalogueDetailExecutor")
    public ThreadPoolTaskExecutor genCatalogueDetailExecutor(MdcTaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("GenCatalogueDetailExecutor-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
