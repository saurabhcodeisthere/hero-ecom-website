package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Only responsible for the payment.commands exchange declaration.
 *
 * order-service is the PUBLISHER of payment commands.
 * When a customer places an order, order-service sends a ProcessPaymentCommand
 * here — payment-service picks it up and initiates checkout.
 *
 * WHY DirectExchange?
 * Commands go to exactly one target — payment-service.
 * No wildcard routing needed. Direct is explicit and sufficient.
 *
 * WHY declare exchange here AND in payment-service?
 * Idempotent declaration — RabbitMQ creates it once, ignores subsequent
 * declarations with the same name and settings. This ensures the exchange
 * exists regardless of which service starts first.
 *
 * Reason to change: only if exchange name or type changes.
 */
@Configuration
public class PaymentCommandPublisherConfig {

    public static final String EXCHANGE_NAME = "payment.commands";
    public static final String ROUTING_KEY   = "payment.process";

    @Bean
    public DirectExchange paymentCommandsExchange() {

        return new DirectExchange(EXCHANGE_NAME, true, false);
    }
}
