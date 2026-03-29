package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Only responsible for the payment.events exchange.
 *
 * payment-service is the PUBLISHER of payment results.
 * After processing a payment (success or failure), payment-service
 * publishes a PaymentResultEvent here.
 *
 * Routing keys published:
 *   "payment.success" → order captured, order can be confirmed
 *   "payment.failed"  → card declined, order must be cancelled + stock restored
 *
 * WHY TopicExchange?
 * Multiple consumers can selectively bind:
 *   order-service       → binds with "payment.#"  (receives all payment events)
 *   notification-service → binds with "payment.#"  (could send payment receipts)
 *   analytics-service   → binds with "payment.#"  (could track revenue)
 *
 * payment-service only declares the exchange.
 * Each consumer declares its own queue and binding.
 * This follows ISP — payment-service has no knowledge of who listens.
 *
 * WHY the exchange is declared here AND in order-service?
 * Both services declare the same exchange name.
 * RabbitMQ creates it once — subsequent declarations are no-ops (idempotent).
 * This ensures the exchange exists regardless of which service starts first.
 */
@Configuration
public class PaymentEventsExchangeConfig {

    public static final String EXCHANGE_NAME = "payment.events";

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }
}
