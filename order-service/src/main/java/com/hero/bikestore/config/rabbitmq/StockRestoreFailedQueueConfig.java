package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the error queue for stock restore failures during order cancellation.
 *
 * WHY DIRECT EXCHANGE (not Topic)?
 * ──────────────────────────────────
 * This is a point-to-point error channel. Exactly one producer (TimeoutServiceImpl)
 * and one consumer (ops team / future retry service). No routing patterns needed.
 * DirectExchange is the simplest correct choice.
 *
 * WHY NO DLQ FOR THIS QUEUE?
 * ───────────────────────────
 * This queue IS effectively a dead letter queue — it already holds failed operations.
 * Adding another DLQ layer on top would be redundant. If consuming from this
 * queue fails, the message stays here (durable) for manual inspection.
 *
 * DIFFERENCE FROM NOTIFICATION-SERVICE DLQ:
 * ───────────────────────────────────────────
 * notification-service uses the NATIVE RabbitMQ DLQ pattern:
 *   Message already in queue → consumer throws → RabbitMQ auto-routes to DLQ
 *
 * This queue uses the EXPLICIT PUBLISH pattern:
 *   Failure happens in Java code (HTTP call fails in catch block)
 *   No message existed in any queue → we manually publish the failure here
 */
@Configuration
public class StockRestoreFailedQueueConfig {

    public static final String QUEUE_NAME    = "stock.restore.failed.queue";
    public static final String EXCHANGE_NAME = "stock.restore.failed.exchange";
    public static final String ROUTING_KEY   = "stock.restore.failed";

    @Bean
    public Queue stockRestoreFailedQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public DirectExchange stockRestoreFailedExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding stockRestoreFailedBinding(Queue stockRestoreFailedQueue,
                                              DirectExchange stockRestoreFailedExchange) {
        return BindingBuilder
                .bind(stockRestoreFailedQueue)
                .to(stockRestoreFailedExchange)
                .with(ROUTING_KEY);
    }
}
