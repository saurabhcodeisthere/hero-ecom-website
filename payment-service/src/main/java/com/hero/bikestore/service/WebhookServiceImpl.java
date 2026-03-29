package com.hero.bikestore.service;

import com.hero.bikestore.dto.PaymentResultEvent;
import com.hero.bikestore.entity.Payment;
import com.hero.bikestore.enums.PaymentStatus;
import com.hero.bikestore.publisher.PaymentEventPublisher;
import com.hero.bikestore.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processes the result of a payment checkout — mock or real.
 *
 * Flow for SUCCESS:
 *   1. Look up Payment by gatewayPaymentId
 *   2. Update status → SUCCESS, set transactionId
 *   3. Save to DB
 *   4. Publish PaymentResultEvent (routing key: "payment.success")
 *   5. order-service PaymentReplyListener confirms the order
 *
 * Flow for FAILURE:
 *   1. Look up Payment by gatewayPaymentId
 *   2. Update status → FAILED, set failureReason
 *   3. Save to DB
 *   4. Publish PaymentResultEvent (routing key: "payment.failed")
 *   5. order-service PaymentReplyListener restores stock + cancels order
 *
 * WHY gatewayPaymentId and not DB id?
 * The mock checkout URL (and Razorpay webhook) only knows the gateway's own ID.
 * We stored it in the Payment record at initiation so we can look it up here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final PaymentRepository    paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public void processSuccess(String gatewayPaymentId) {
        log.info("[WebhookServiceImpl] processSuccess | ENTER — gatewayPaymentId={}", gatewayPaymentId);

        log.info("[WebhookServiceImpl] STEP 1/3 — Looking up Payment by gatewayPaymentId={}", gatewayPaymentId);
        Payment payment = findPayment(gatewayPaymentId);
        log.info("[WebhookServiceImpl] STEP 1/3 — Payment found | dbId={} orderId={} orderNumber={} currentStatus={}",
                payment.getId(), payment.getOrderId(), payment.getOrderNumber(), payment.getStatus());

        // Generate a mock transaction ID (Razorpay provides a real one in prod)
        String transactionId = "mock-txn-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[WebhookServiceImpl] STEP 2/3 — Updating Payment status INITIATED → SUCCESS | transactionId={}", transactionId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId);
        paymentRepository.save(payment);
        log.info("[WebhookServiceImpl] STEP 2/3 — Payment updated in DB | orderId={} status=SUCCESS transactionId={}",
                payment.getOrderId(), transactionId);

        // Publish event → order-service will confirm the order
        log.info("[WebhookServiceImpl] STEP 3/3 — Publishing PaymentResultEvent to RabbitMQ | orderId={} status=SUCCESS",
                payment.getOrderId());
        PaymentResultEvent event = PaymentResultEvent.builder()
                .orderId(payment.getOrderId())
                .orderNumber(payment.getOrderNumber())
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .build();

        paymentEventPublisher.publish(event);
        log.info("[WebhookServiceImpl] STEP 3/3 — PaymentResultEvent published | orderId={} transactionId={}",
                payment.getOrderId(), transactionId);

        log.info("[WebhookServiceImpl] processSuccess | EXIT — orderId={} transactionId={}", payment.getOrderId(), transactionId);
    }

    @Override
    @Transactional
    public void processFailure(String gatewayPaymentId, String failureReason) {
        log.info("[WebhookServiceImpl] processFailure | ENTER — gatewayPaymentId={} reason={}", gatewayPaymentId, failureReason);

        log.info("[WebhookServiceImpl] STEP 1/3 — Looking up Payment by gatewayPaymentId={}", gatewayPaymentId);
        Payment payment = findPayment(gatewayPaymentId);
        log.info("[WebhookServiceImpl] STEP 1/3 — Payment found | dbId={} orderId={} orderNumber={} currentStatus={}",
                payment.getId(), payment.getOrderId(), payment.getOrderNumber(), payment.getStatus());

        log.info("[WebhookServiceImpl] STEP 2/3 — Updating Payment status INITIATED → FAILED | reason={}", failureReason);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);
        log.info("[WebhookServiceImpl] STEP 2/3 — Payment updated in DB | orderId={} status=FAILED reason={}",
                payment.getOrderId(), failureReason);

        // Publish event → order-service will restore stock and cancel the order
        log.info("[WebhookServiceImpl] STEP 3/3 — Publishing PaymentResultEvent to RabbitMQ | orderId={} status=FAILED",
                payment.getOrderId());
        PaymentResultEvent event = PaymentResultEvent.builder()
                .orderId(payment.getOrderId())
                .orderNumber(payment.getOrderNumber())
                .status(PaymentStatus.FAILED)
                .failureReason(failureReason)
                .build();

        paymentEventPublisher.publish(event);
        log.info("[WebhookServiceImpl] STEP 3/3 — PaymentResultEvent published | orderId={} reason={}",
                payment.getOrderId(), failureReason);

        log.info("[WebhookServiceImpl] processFailure | EXIT — orderId={} reason={}", payment.getOrderId(), failureReason);
    }

    private Payment findPayment(String gatewayPaymentId) {
        return paymentRepository.findByGatewayPaymentId(gatewayPaymentId)
                .orElseThrow(() -> {
                    log.error("Payment not found for gatewayPaymentId={}", gatewayPaymentId);
                    return new RuntimeException("Payment not found: " + gatewayPaymentId);
                });
    }
}
