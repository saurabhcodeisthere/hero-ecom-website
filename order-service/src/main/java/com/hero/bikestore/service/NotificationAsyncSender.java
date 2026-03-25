package com.hero.bikestore.service;

import com.hero.bikestore.client.NotificationServiceClient;
import com.hero.bikestore.dto.event.OrderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * [DISABLED — KEPT FOR REFERENCE]
 *
 * Original HTTP-based async notification sender.
 * Replaced by RabbitMQ event publishing (RabbitMQEventPublisher).
 *
 * WHY KEPT:
 *   - Documents the previous approach for learning and comparison
 *   - Can be re-enabled by restoring @Component if RabbitMQ is removed
 *
 * WHY DISABLED:
 *   - Direct HTTP coupling: if notification-service is down, email is lost forever
 *   - No retry mechanism — one failure = one lost notification
 *   - Replaced by RabbitMQ which provides durability, retry and decoupling
 *
 * TO RE-ENABLE: restore @Component below
 */
// @Component   ← DISABLED: replaced by RabbitMQEventPublisher
@Deprecated
@RequiredArgsConstructor
@Slf4j
public class NotificationAsyncSender {

    private final NotificationServiceClient notificationServiceClient;

    /**
     * Sends an order event notification in a background thread.
     * executor = "notificationTaskExecutor" defined in AsyncConfig.
     *
     * @param event the fully built OrderNotificationEvent
     */
    @Async("notificationTaskExecutor")
    public void send(OrderNotificationEvent event) {
        try {
            log.info("Sending notification event type={} orderId={} (background thread={})",
                    event.getType(), event.getOrderId(), Thread.currentThread().getName());

            notificationServiceClient.sendOrderEvent(event);

            log.info("Notification sent successfully for orderId={}", event.getOrderId());

        } catch (Exception e) {
            // Best-effort: notification failure must NEVER affect order state.
            // Log the error and silently continue.
            log.error("Failed to send notification for orderId={} type={}: {}",
                    event.getOrderId(), event.getType(), e.getMessage());
        }
    }
}
