package com.hero.bikestore.publisher;

import com.hero.bikestore.dto.payment.ProcessPaymentCommand;

/**
 * DIP — abstraction for publishing payment commands.
 *
 * OrderService depends on this interface, NOT on RabbitMQ directly.
 * Same pattern as EventPublisher for order notifications.
 *
 * Benefit:
 *   If the payment command channel changes (RabbitMQ → HTTP → Kafka),
 *   only the implementation changes — OrderService stays untouched.
 */
public interface PaymentCommandPublisher {

    void publish(ProcessPaymentCommand command);
}
