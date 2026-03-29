package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Only responsible for the payment reply queue owned by order-service.
 *
 * order-service owns this queue — it is the CONSUMER of payment results.
 * payment-service publishes to payment.events exchange.
 * RabbitMQ routes messages matching "payment.#" to this queue.
 *
 * WHY "payment.#" as binding pattern?
 * Catches both routing keys published by payment-service:
 *   "payment.success" → order should be confirmed
 *   "payment.failed"  → order should be cancelled + stock restored
 *
 * PaymentReplyListener subscribes to this queue.
 *
 * Reason to change: only if queue name or binding pattern changes.
 */
@Configuration
public class PaymentReplyQueueConfig {

    public static final String QUEUE_NAME   = "order.payment.reply.queue";
    public static final String ROUTING_KEY  = "payment.#";

    @Bean
    public Queue paymentReplyQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding paymentReplyBinding(Queue paymentReplyQueue,
                                        TopicExchange paymentEventsExchange) {
        return BindingBuilder
                .bind(paymentReplyQueue)
                .to(paymentEventsExchange)
                .with(ROUTING_KEY);
    }
}
