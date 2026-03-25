package com.hero.bikestore.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification-service — consumer side.
 *
 * SINGLE RESPONSIBILITY:
 * ───────────────────────
 * Declares ONLY what notification-service owns:
 *   1. notification.queue       — where order events are delivered
 *   2. notification.dlq         — where failed events go after max retries
 *   3. Binding                  — connects the exchange to notification.queue
 *   4. DLQ Exchange + Binding   — connects failed messages to notification.dlq
 *
 * WHY DOES NOTIFICATION-SERVICE ALSO DECLARE THE EXCHANGE?
 * ──────────────────────────────────────────────────────────
 * Both order-service and notification-service declare "order.events" exchange.
 * RabbitMQ is idempotent — if the exchange already exists with same settings,
 * the second declaration is a no-op. This ensures the exchange exists
 * even if notification-service starts before order-service.
 *
 * BINDING PATTERN — "order.#"
 * ─────────────────────────────
 * "#" matches zero or more words after "order."
 * This means notification.queue receives ALL order events:
 *   order.placed, order.confirmed, order.shipped,
 *   order.delivered, order.cancelled
 *
 * Future services (loyalty, inventory) use narrower patterns
 * like "order.delivered" or "order.cancelled" on their own queues.
 *
 * DEAD LETTER QUEUE (DLQ):
 * ─────────────────────────
 * When message processing fails and max retries are exhausted,
 * RabbitMQ automatically moves the message to notification.dlq.
 * Failed messages can be inspected in RabbitMQ Management UI
 * at http://localhost:15672 — no email is silently lost.
 */
@Configuration
public class RabbitMQConfig {

    // ── Main Queue ─────────────────────────────────────────────────────────
    public static final String QUEUE_NAME    = "notification.queue";

    // ── Exchange (same name as order-service — idempotent) ─────────────────
    public static final String EXCHANGE_NAME = "order.events";

    // ── Routing key pattern — subscribes to all order events ───────────────
    public static final String ROUTING_KEY   = "order.#";

    // ── Dead Letter Queue ──────────────────────────────────────────────────
    public static final String DLQ_NAME      = "notification.dlq";
    public static final String DLQ_EXCHANGE  = "notification.dlq.exchange";

    /**
     * Main queue — receives all order events from order.events exchange.
     *
     * x-dead-letter-exchange: on processing failure,
     * RabbitMQ routes the message to the DLQ exchange automatically.
     * durable = true → queue survives RabbitMQ restart, no messages lost.
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .build();
    }

    /**
     * Topic exchange — same declaration as order-service.
     * Idempotent: RabbitMQ won't create a duplicate.
     */
    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Binding — connects notification.queue to order.events exchange.
     *
     * Pattern "order.#" means:
     *   order.placed     ✅
     *   order.confirmed  ✅
     *   order.shipped    ✅
     *   order.delivered  ✅
     *   order.cancelled  ✅
     *   payment.placed   ❌ (different prefix — not routed here)
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue,
                                       TopicExchange orderEventsExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(orderEventsExchange)
                .with(ROUTING_KEY);
    }

    // ── Dead Letter Queue Setup ────────────────────────────────────────────

    /**
     * DLQ — receives messages that failed processing after max retries.
     * Durable so failed messages are never lost across restarts.
     * Inspect via RabbitMQ Management UI → Queues → notification.dlq
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    /**
     * DLQ Exchange — direct exchange for dead lettered messages.
     * Direct (not topic) because routing is exact — no pattern matching needed.
     */
    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    /**
     * DLQ Binding — connects notification.dlq to the DLQ exchange.
     * Routing key matches the original queue name by convention.
     */
    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange dlqExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(dlqExchange)
                .with(QUEUE_NAME);
    }

    /**
     * JSON message converter — deserialises incoming JSON back to
     * OrderNotificationEvent. Must match the converter used by the publisher.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Listener container factory — controls what happens when processing fails.
     *
     * WHY defaultRequeueRejected = false?
     * ─────────────────────────────────────
     * By default Spring AMQP requeues a failed message back to the SAME queue.
     * This creates an infinite retry loop:
     *   fail → requeue → fail → requeue → ∞
     *
     * Setting this to false changes the behaviour:
     *   fail → REJECT (NACK without requeue)
     *        → RabbitMQ sees rejection
     *        → routes to x-dead-letter-exchange (notification.dlq.exchange)
     *        → message lands in notification.dlq ✅
     *
     * Without this bean Spring Boot uses its default factory
     * which has defaultRequeueRejected = true — DLQ never receives anything.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);  // ← reject → DLQ, not requeue
        return factory;
    }
}
