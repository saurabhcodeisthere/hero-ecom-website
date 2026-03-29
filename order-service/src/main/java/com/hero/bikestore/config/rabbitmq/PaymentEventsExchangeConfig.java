package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Only responsible for the payment.events exchange declaration.
 *
 * order-service is a CONSUMER of payment results.
 * payment-service publishes PaymentResultEvent here after checkout completes.
 *
 * WHY TopicExchange?
 * Multiple consumers can bind selectively:
 *   order-service        → "payment.#"       (receives all payment results)
 *   notification-service → "payment.#"       (could send payment receipts in future)
 *
 * Idempotent — payment-service also declares this exchange.
 * RabbitMQ creates it once, subsequent declarations are no-ops.
 *
 * Reason to change: only if exchange name or type changes.
 */
@Configuration
public class PaymentEventsExchangeConfig {

    public static final String EXCHANGE_NAME = "payment.events";

    @Bean
    public TopicExchange paymentEventsExchange() {

        return new TopicExchange(EXCHANGE_NAME, true, false);
    }
}
