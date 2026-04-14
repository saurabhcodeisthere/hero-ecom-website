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
 *   2. Guard: skip if payment is not INITIATED (EXPIRED/FAILED/SUCCESS)
 *   3. Update status → SUCCESS, set transactionId
 *   4. Save to DB
 *   5. Publish PaymentResultEvent (routing key: "payment.success")
 *   6. order-service PaymentReplyListener confirms the order
 *
 * Flow for FAILURE:
 *   1. Look up Payment by gatewayPaymentId
 *   2. Guard: skip if payment is not INITIATED
 *   3. Update status → FAILED, set failureReason
 *   4. Save to DB
 *   5. Publish PaymentResultEvent (routing key: "payment.failed")
 *   6. order-service PaymentReplyListener restores stock + cancels order
 *
 * WHY THE GUARD IS CRITICAL:
 * ───────────────────────────
 * Without the guard, a race condition exists:
 *   1. Timeout job sets payment = EXPIRED, order = CANCELLED
 *   2. Customer opens their old payment URL (still in browser)
 *   3. Customer clicks Pay
 *   4. processSuccess() overwrites EXPIRED → SUCCESS
 *   5. PaymentResultEvent published → order-service tries to CONFIRM a CANCELLED order
 *
 * The guard at Step 2 short-circuits the whole flow for non-INITIATED payments.
 * The MockCheckoutController ALSO checks on page load, but a direct POST to
 * /mock/checkout/{id}/success bypasses the page — so this guard is the final defence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final PaymentRepository     paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public void processSuccess(String gatewayPaymentId) {
        log.info("[WebhookServiceImpl] processSuccess | ENTER — gatewayPaymentId={}", gatewayPaymentId);

        log.info("[WebhookServiceImpl] STEP 1/3 — Looking up Payment | gatewayPaymentId={}", gatewayPaymentId);
        Payment payment = findPayment(gatewayPaymentId);
        log.info("[WebhookServiceImpl] STEP 1/3 — Payment found | dbId={} orderId={} orderNumber={} currentStatus={}",
                payment.getId(), payment.getOrderId(), payment.getOrderNumber(), payment.getStatus());

        // GUARD — only process if payment is still INITIATED
        // EXPIRED = timeout job already cancelled the order → do not confirm it
        // SUCCESS = already processed (duplicate webhook) → skip
        // FAILED  = already processed as failure → skip
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            log.warn("[WebhookServiceImpl] processSuccess — SKIPPED. Payment is not INITIATED | " +
                     "orderId={} currentStatus={}. No saga event will be published.",
                    payment.getOrderId(), payment.getStatus());
            return;
        }

        String transactionId = "mock-txn-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[WebhookServiceImpl] STEP 2/3 — Updating Payment status INITIATED → SUCCESS | transactionId={}",
                transactionId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId);
        paymentRepository.save(payment);
        log.info("[WebhookServiceImpl] STEP 2/3 — Payment updated | orderId={} status=SUCCESS transactionId={}",
                payment.getOrderId(), transactionId);

        log.info("[WebhookServiceImpl] STEP 3/3 — Publishing PaymentResultEvent | orderId={} status=SUCCESS",
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

        log.info("[WebhookServiceImpl] processSuccess | EXIT — orderId={} transactionId={}",
                payment.getOrderId(), transactionId);
    }

    @Override
    @Transactional
    public void processFailure(String gatewayPaymentId, String failureReason) {
        log.info("[WebhookServiceImpl] processFailure | ENTER — gatewayPaymentId={} reason={}", gatewayPaymentId, failureReason);

        log.info("[WebhookServiceImpl] STEP 1/3 — Looking up Payment | gatewayPaymentId={}", gatewayPaymentId);
        Payment payment = findPayment(gatewayPaymentId);
        log.info("[WebhookServiceImpl] STEP 1/3 — Payment found | dbId={} orderId={} orderNumber={} currentStatus={}",
                payment.getId(), payment.getOrderId(), payment.getOrderNumber(), payment.getStatus());

        // GUARD — only process if payment is still INITIATED
        // EXPIRED = timeout job already cancelled the order → don't double-cancel
        // FAILED  = already processed (duplicate webhook) → skip
        // SUCCESS = already confirmed → cannot fail a confirmed payment
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            log.warn("[WebhookServiceImpl] processFailure — SKIPPED. Payment is not INITIATED | " +
                     "orderId={} currentStatus={}. No saga event will be published.",
                    payment.getOrderId(), payment.getStatus());
            return;
        }

        log.info("[WebhookServiceImpl] STEP 2/3 — Updating Payment status INITIATED → FAILED | reason={}",
                failureReason);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);
        log.info("[WebhookServiceImpl] STEP 2/3 — Payment updated | orderId={} status=FAILED reason={}",
                payment.getOrderId(), failureReason);

        log.info("[WebhookServiceImpl] STEP 3/3 — Publishing PaymentResultEvent | orderId={} status=FAILED",
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
