package com.hero.bikestore.publisher;

import com.hero.bikestore.config.rabbitmq.PaymentEventsExchangeConfig;
import com.hero.bikestore.dto.PaymentResultEvent;
import com.hero.bikestore.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of PaymentEventPublisher.
 *
 * Publishes PaymentResultEvent to payment.events exchange.
 * Routing key is derived from payment status:
 *   SUCCESS → "payment.success"
 *   FAILED  → "payment.failed"
 *
 * WHY DYNAMIC ROUTING KEY?
 * order-service can choose to listen to specific outcomes:
 *   "payment.success" only  → if it only cares about confirmations
 *   "payment.#"             → if it wants all payment results (our case)
 *
 * notification-service can also bind to "payment.#" later
 * to send payment receipts — without any changes to this class (OCP).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPaymentEventPublisher implements PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(PaymentResultEvent event) {
        String routingKey = toRoutingKey(event.getStatus());

        log.info("[RabbitMQPaymentEventPublisher] publish | ENTER");
        log.info("[RabbitMQPaymentEventPublisher] Target exchange={} routingKey={}",
                PaymentEventsExchangeConfig.EXCHANGE_NAME, routingKey);
        log.info("[RabbitMQPaymentEventPublisher] Payload | orderId={} orderNumber={} status={} transactionId={} failureReason={}",
                event.getOrderId(), event.getOrderNumber(), event.getStatus(),
                event.getTransactionId(), event.getFailureReason());

        rabbitTemplate.convertAndSend(
                PaymentEventsExchangeConfig.EXCHANGE_NAME,
                routingKey,
                event
        );

        log.info("[RabbitMQPaymentEventPublisher] publish | EXIT — message sent to RabbitMQ | orderId={} routingKey={}",
                event.getOrderId(), routingKey);
    }

    /**
     * Derives routing key from payment status.
     * SUCCESS → "payment.success"
     * FAILED  → "payment.failed"
     */
    private String toRoutingKey(PaymentStatus status) {
        return "payment." + status.name().toLowerCase();
    }
}
