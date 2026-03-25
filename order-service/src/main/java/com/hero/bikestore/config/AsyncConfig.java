package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables @Async support and configures a dedicated thread pool
 * for background notification sending tasks.
 *
 * WHY does this live in order-service and NOT notification-service?
 * ─────────────────────────────────────────────────────────────────
 * @Async is used by NotificationAsyncSender which lives in order-service.
 * order-service is the one that does NOT want to wait for the email —
 * it hands off the HTTP call to a background thread and returns 201 immediately.
 *
 * notification-service is a plain synchronous REST service.
 * It receives an HTTP request, sends an email, returns 200.
 * It has no async behaviour and needs no thread pool.
 *
 * WHY a dedicated thread pool instead of Spring's default?
 * ─────────────────────────────────────────────────────────
 * Spring's default SimpleAsyncTaskExecutor creates a brand new thread
 * for every @Async call and never reuses or limits them.
 * Under load (1000 orders/min) this exhausts memory.
 *
 * ThreadPoolTaskExecutor gives us:
 *   corePoolSize  : 5 threads always alive, ready to pick up tasks immediately
 *   maxPoolSize   : grows to 10 under burst load, then stops
 *   queueCapacity : up to 100 tasks wait in queue when all 10 threads are busy
 *   threadPrefix  : "notification-" prefix makes threads easy to spot in logs
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationTaskExecutor")
    public TaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }
}
