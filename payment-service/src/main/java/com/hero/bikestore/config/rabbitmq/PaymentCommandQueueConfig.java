package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Only responsible for the payment command queue.
 *
 * payment-service is the CONSUMER of commands.
 * order-service publishes a ProcessPaymentCommand here
 * when a customer places an order.
 *
 * Flow:
 *   order-service
 *     → payment.commands exchange
 *     → routing key "payment.process"
 *     → payment.command.queue
 *     → PaymentCommandListener (consumes here)
 *
 * WHY DirectExchange?
 * Commands go to exactly one target — payment-service.
 * No wildcard matching needed. Direct routing is sufficient and explicit.
 *
 * WHY no DLQ here?
 * If payment command processing fails, the order-service saga handles
 * compensation via PaymentResultEvent (FAILED). A DLQ would duplicate
 * that responsibility.
 */
@Configuration
public class PaymentCommandQueueConfig {

    public static final String EXCHANGE_NAME = "payment.commands";
    public static final String QUEUE_NAME    = "payment.command.queue";
    public static final String ROUTING_KEY   = "payment.process";

    @Bean
    public DirectExchange paymentCommandsExchange() {
        // durable=true  → survives RabbitMQ restart
        // autoDelete=false → stays even when no consumers connected
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue paymentCommandQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding paymentCommandBinding(Queue paymentCommandQueue,
                                          DirectExchange paymentCommandsExchange) {
        return BindingBuilder
                .bind(paymentCommandQueue)
                .to(paymentCommandsExchange)
                .with(ROUTING_KEY);
    }
}
