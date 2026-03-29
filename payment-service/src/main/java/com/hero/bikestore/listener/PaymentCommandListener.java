package com.hero.bikestore.listener;

import com.hero.bikestore.config.rabbitmq.PaymentCommandQueueConfig;
import com.hero.bikestore.dto.ProcessPaymentCommand;
import com.hero.bikestore.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Entry point for payment commands from order-service.
 *
 * SRP: Only receives the message and delegates to PaymentService.
 *      No business logic lives here — keeps the boundary clean.
 *
 * Flow:
 *   order-service publishes ProcessPaymentCommand to payment.commands exchange
 *   → RabbitMQ routes via "payment.process" key to payment.command.queue
 *   → This listener receives and deserializes the command
 *   → Delegates to PaymentService.initiatePayment()
 *
 * NO interface on this class — it is an infrastructure adapter tied to RabbitMQ.
 * If messaging technology changes, this class is replaced entirely, not swapped.
 * The interface lives on PaymentService (the business logic it delegates to).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = PaymentCommandQueueConfig.QUEUE_NAME)
    public void onProcessPayment(ProcessPaymentCommand command) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[PaymentCommandListener] onProcessPayment | ENTER");
        log.info("[PaymentCommandListener] RabbitMQ message received from queue: {}", PaymentCommandQueueConfig.QUEUE_NAME);
        log.info("[PaymentCommandListener] orderId={} orderNumber={} amount={} userEmail={}",
                command.getOrderId(), command.getOrderNumber(), command.getAmount(), command.getUserEmail());
        log.info("[PaymentCommandListener] Delegating to PaymentService.initiatePayment()");

        paymentService.initiatePayment(command);

        log.info("[PaymentCommandListener] onProcessPayment | EXIT — orderId={} fully processed", command.getOrderId());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
