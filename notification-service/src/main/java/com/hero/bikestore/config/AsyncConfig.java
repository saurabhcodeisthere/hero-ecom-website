package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables @Async support and configures a dedicated thread pool for
 * background email sending tasks.
 *
 * WHY a dedicated thread pool?
 * ────────────────────────────
 * By default, @Async uses Spring's SimpleAsyncTaskExecutor — it creates
 * a new thread for EVERY task with no pooling or queue limits.
 * Under load this can exhaust system resources.
 *
 * A ThreadPoolTaskExecutor gives us:
 *   - corePoolSize:   5 threads always alive, ready to pick up tasks
 *   - maxPoolSize:    10 threads max under high load
 *   - queueCapacity: 100 tasks can wait in queue before rejection
 *   - Named threads:  easier to spot in logs and thread dumps
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-async-");
        executor.initialize();
        return executor;
    }
}
