package com.hero.bikestore.listener;

import com.hero.bikestore.config.RabbitMQConfig;
import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer — listens to notification.queue and delegates
 * each message to NotificationService for processing.
 *
 * SINGLE RESPONSIBILITY:
 * ───────────────────────
 * One job only — receive messages from the queue and hand them off.
 * No email logic. No template logic. No routing logic. Just consume + delegate.
 *
 * HOW @RabbitListener WORKS:
 * ───────────────────────────
 * Spring AMQP keeps a persistent connection to RabbitMQ.
 * When a message arrives in notification.queue, Spring:
 *   1. Deserialises the JSON payload → OrderNotificationEvent (via Jackson2JsonMessageConverter)
 *   2. Calls this onOrderEvent() method with the deserialised object
 *   3. If the method returns normally → message acknowledged (removed from queue)
 *   4. If the method throws an exception → message requeued for retry
 *   5. After max retries → message moved to notification.dlq automatically
 *
 * WHY SEPARATE FROM NotificationService?
 * ────────────────────────────────────────
 * NotificationService has ONE job — route event to correct handler.
 * This class has ONE job — consume from queue.
 * Keeping them separate satisfies SRP and makes each independently testable.
 *
 * Previously: HTTP POST → NotificationController → NotificationService
 * Now:        Queue     → NotificationListener   → NotificationService
 *
 * NotificationService is UNCHANGED — it doesn't know or care
 * whether the event arrived via HTTP or RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    /**
     * Consumes an order notification event from the queue.
     *
     * Binding pattern "order.#" in RabbitMQConfig ensures this queue
     * receives all order.* routing keys published by order-service:
     *   order.placed, order.confirmed, order.shipped,
     *   order.delivered, order.cancelled
     *
     * @param event deserialised OrderNotificationEvent from the queue
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onOrderEvent(OrderNotificationEvent event) {
        log.info("Received from queue: type={} orderId={} orderNumber={}",
                event.getType(), event.getOrderId(), event.getOrderNumber());

        notificationService.handleOrderEvent(event);

        log.info("Event processed successfully: type={} orderId={}",
                event.getType(), event.getOrderId());
    }
}
