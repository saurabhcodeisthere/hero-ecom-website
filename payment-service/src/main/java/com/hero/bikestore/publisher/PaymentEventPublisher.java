package com.hero.bikestore.publisher;

import com.hero.bikestore.dto.PaymentResultEvent;

/**
 * DIP — abstraction for publishing payment result events.
 *
 * PaymentService depends on this interface, NOT on RabbitMQ directly.
 * Same pattern as EventPublisher in order-service.
 *
 * Benefit:
 *   If messaging infrastructure changes (RabbitMQ → Kafka),
 *   only the implementation changes — PaymentService stays untouched.
 */
public interface PaymentEventPublisher {

    void publish(PaymentResultEvent event);
}
