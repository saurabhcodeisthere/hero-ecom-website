package com.hero.bikestore.listener;

import com.hero.bikestore.config.rabbitmq.PaymentReplyQueueConfig;
import com.hero.bikestore.dto.payment.PaymentResultEvent;
import com.hero.bikestore.dto.payment.PaymentStatus;
import com.hero.bikestore.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Entry point for payment results from payment-service.
 *
 * SRP: Only receives the message and delegates to SagaOrchestrator.
 *      No business logic lives here.
 *
 * NO interface on this class — it is an infrastructure adapter tied to RabbitMQ.
 * The interface lives on SagaOrchestrator (the business logic it delegates to).
 *
 * TYPE PRECEDENCE:
 * Uses INFERRED type precedence (configured in PaymentRabbitListenerConfig).
 * Spring deserializes the JSON into PaymentResultEvent using this method's
 * parameter type — ignoring the __TypeId__ header from payment-service.
 * This prevents ClassNotFoundException when payment-service's package differs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReplyListener {

    private final SagaOrchestrator sagaOrchestrator;

    @RabbitListener(queues = PaymentReplyQueueConfig.QUEUE_NAME)
    public void onPaymentResult(PaymentResultEvent event) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[PaymentReplyListener] onPaymentResult | ENTER");
        log.info("[PaymentReplyListener] RabbitMQ message received from queue: {}", PaymentReplyQueueConfig.QUEUE_NAME);
        log.info("[PaymentReplyListener] orderId={} orderNumber={} status={} transactionId={} failureReason={}",
                event.getOrderId(), event.getOrderNumber(), event.getStatus(),
                event.getTransactionId(), event.getFailureReason());

        if (event.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[PaymentReplyListener] Status=SUCCESS → routing to SagaOrchestrator.handlePaymentSuccess()");
            sagaOrchestrator.handlePaymentSuccess(event);
        } else {
            log.info("[PaymentReplyListener] Status={} → routing to SagaOrchestrator.handlePaymentFailure()", event.getStatus());
            sagaOrchestrator.handlePaymentFailure(event);
        }

        log.info("[PaymentReplyListener] onPaymentResult | EXIT — orderId={}", event.getOrderId());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
