package com.hero.bikestore.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for order-service — publisher side.
 *
 * SINGLE RESPONSIBILITY:
 * ───────────────────────
 * Declares ONLY the exchange. order-service is a producer —
 * it does not own any queue. Queues are owned by consumer services.
 *
 * WHY TOPIC EXCHANGE?
 * ────────────────────
 * Topic exchange supports wildcard routing key patterns:
 *   *  matches exactly one word
 *   #  matches zero or more words
 *
 * This allows consumers to subscribe selectively:
 *   notification-service → "order.#"          (all order events)
 *   loyalty-service      → "order.delivered"  (only deliveries)
 *   inventory-service    → "order.cancelled"  (only cancellations)
 *
 * Adding a new consumer = new queue + new binding. ZERO changes here.
 * Open/Closed Principle at the infrastructure level.
 *
 * EXCHANGE NAME:
 * ───────────────
 * Shared constant used by RabbitMQEventPublisher to avoid hardcoded strings.
 * notification-service declares the same exchange name so RabbitMQ
 * recognises them as the same exchange (idempotent declaration).
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange ──────────────────────────────────────────────────────────
    public static final String EXCHANGE_NAME = "order.events";

    /**
     * Declares the topic exchange on RabbitMQ.
     * Idempotent — if it already exists with same settings, no error.
     * durable = true → exchange survives RabbitMQ restart.
     */
    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
        //                                      ^^^^  ^^^^^
        //                                   durable  auto-delete=false
    }

    /**
     * JSON message converter — serialises OrderNotificationEvent as JSON.
     *
     * Without this, Spring uses Java serialisation (binary) by default.
     * JSON is human-readable, debuggable in RabbitMQ Management UI,
     * and language-agnostic (future consumers can be non-Java).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
