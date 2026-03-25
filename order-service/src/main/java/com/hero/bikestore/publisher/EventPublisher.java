package com.hero.bikestore.publisher;

import com.hero.bikestore.dto.event.OrderNotificationEvent;

/**
 * Abstraction for publishing order notification events.
 *
 * WHY AN INTERFACE? (Dependency Inversion Principle)
 * ───────────────────────────────────────────────────
 * OrderService depends on THIS interface — not on RabbitMQ, not on HTTP.
 * The concrete implementation (RabbitMQEventPublisher) is injected by Spring.
 *
 * BENEFIT:
 *   Swapping RabbitMQ → Kafka → SQS tomorrow requires:
 *     1. Write a new implementation of this interface
 *     2. Swap the @Component annotation
 *     3. OrderService code = ZERO changes
 *
 * SINGLE RESPONSIBILITY:
 *   This interface has exactly one job — define the publishing contract.
 *   Nothing else.
 */
public interface EventPublisher {

    /**
     * Publishes an order notification event to the message broker.
     *
     * @param event the fully built OrderNotificationEvent to publish
     */
    void publish(OrderNotificationEvent event);
}
