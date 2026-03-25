package com.hero.bikestore.publisher;

import com.hero.bikestore.config.RabbitMQConfig;
import com.hero.bikestore.dto.event.OrderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of EventPublisher.
 *
 * SINGLE RESPONSIBILITY:
 * ───────────────────────
 * One job only — take an event and put it on the RabbitMQ exchange.
 * No email logic. No business logic. No HTTP. Just publish.
 *
 * HOW ROUTING KEY IS DERIVED:
 * ────────────────────────────
 * The routing key is derived from the event type enum:
 *   ORDER_PLACED    → "order.placed"
 *   ORDER_CONFIRMED → "order.confirmed"
 *   ORDER_SHIPPED   → "order.shipped"
 *   ORDER_DELIVERED → "order.delivered"
 *   ORDER_CANCELLED → "order.cancelled"
 *
 * This allows notification-service (and future services) to subscribe
 * selectively using binding patterns like "order.#" or "order.cancelled".
 *
 * WHY NOT A FIXED ROUTING KEY?
 * ─────────────────────────────
 * A fixed key like "order.event" forces every consumer to receive ALL events
 * and filter internally. Dynamic keys let RabbitMQ do the filtering — cleaner,
 * more scalable, and future-proof for adding new consumers.
 *
 * DEPENDENCY INVERSION:
 * ──────────────────────
 * OrderService injects EventPublisher (interface) — not this class directly.
 * This class is the implementation detail Spring wires behind the interface.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes the event to the order.events topic exchange.
     * Routing key is dynamically derived from event type for selective consumption.
     *
     * convertAndSend serialises the event as JSON (via Jackson2JsonMessageConverter)
     * and delivers it to the exchange. RabbitMQ routes it to the correct queue(s)
     * based on the binding patterns declared in each consumer service.
     */
    @Override
    public void publish(OrderNotificationEvent event) {
        String routingKey = toRoutingKey(event);

        log.info("Publishing event to RabbitMQ — exchange={} routingKey={} orderId={}",
                RabbitMQConfig.EXCHANGE_NAME, routingKey, event.getOrderId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,   // order.events
                routingKey,                      // order.placed / order.confirmed / ...
                event                            // serialised as JSON
        );

        log.info("Event published successfully — type={} orderId={}",
                event.getType(), event.getOrderId());
    }

    /**
     * Converts OrderEventType enum to dot-separated routing key.
     *
     * Example:
     *   ORDER_PLACED    → "order.placed"
     *   ORDER_CANCELLED → "order.cancelled"
     */
    private String toRoutingKey(OrderNotificationEvent event) {
        return event.getType()
                .name()                   // "ORDER_PLACED"
                .toLowerCase()            // "order_placed"
                .replace("_", ".");       // "order.placed"
    }
}
