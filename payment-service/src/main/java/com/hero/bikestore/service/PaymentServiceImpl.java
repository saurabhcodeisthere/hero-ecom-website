package com.hero.bikestore.service;

import com.hero.bikestore.dto.PaymentInitiationResult;
import com.hero.bikestore.dto.ProcessPaymentCommand;
import com.hero.bikestore.entity.Payment;
import com.hero.bikestore.enums.PaymentStatus;
import com.hero.bikestore.processor.PaymentProcessor;
import com.hero.bikestore.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the payment initiation flow.
 *
 * SRP: This class owns only the payment initiation business logic.
 *      - Does not know about RabbitMQ (depends on PaymentProcessor interface)
 *      - Does not know about Razorpay (depends on PaymentProcessor interface)
 *      - Does not know about HTTP (no controller logic here)
 *
 * Flow:
 *   1. Build and save Payment record with status INITIATED
 *   2. Call PaymentProcessor.initiate() to get checkout URL
 *      (MockPaymentProcessor in dev, RazorpayPaymentProcessor in prod)
 *   3. Update Payment record with the checkout URL
 *   4. Log the URL — developer opens this to simulate payment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository  paymentRepository;
    private final PaymentProcessor   paymentProcessor;

    @Override
    @Transactional
    public PaymentInitiationResult initiatePayment(ProcessPaymentCommand command) {
        log.info("[PaymentServiceImpl] initiatePayment | ENTER");
        log.info("[PaymentServiceImpl] orderId={} orderNumber={} amount={} userEmail={}",
                command.getOrderId(), command.getOrderNumber(), command.getAmount(), command.getUserEmail());

        // Step 1 — Save Payment record with INITIATED status
        log.info("[PaymentServiceImpl] STEP 1/3 — Saving Payment record to DB with status=INITIATED");
        Payment payment = Payment.builder()
                .orderId(command.getOrderId())
                .orderNumber(command.getOrderNumber())
                .amount(command.getAmount())
                .userEmail(command.getUserEmail())
                .userName(command.getUserName())
                .status(PaymentStatus.INITIATED)
                .build();

        paymentRepository.save(payment);
        log.info("[PaymentServiceImpl] STEP 1/3 — Payment record saved | dbId={} orderId={} status=INITIATED",
                payment.getId(), payment.getOrderId());

        // Step 2 — Call PaymentProcessor to get checkout URL
        // Dev:  MockPaymentProcessor returns http://localhost:8086/mock/checkout/{uuid}
        // Prod: RazorpayPaymentProcessor calls Razorpay SDK, returns real checkout URL
        log.info("[PaymentServiceImpl] STEP 2/3 — Calling {} to generate checkout URL",
                paymentProcessor.getClass().getSimpleName());
        PaymentInitiationResult result = paymentProcessor.initiate(command);
        log.info("[PaymentServiceImpl] STEP 2/3 — Checkout URL generated | gatewayPaymentId={} paymentUrl={}",
                result.getPaymentId(), result.getPaymentUrl());

        // Step 3 — Update Payment record with gateway ID and checkout URL
        // gatewayPaymentId is the key used to look up this record when the webhook fires
        // Dev:  UUID embedded in mock checkout URL → http://localhost:8086/mock/checkout/{uuid}
        // Prod: Razorpay orderId embedded in Razorpay checkout URL
        log.info("[PaymentServiceImpl] STEP 3/3 — Updating Payment record with gatewayPaymentId and paymentUrl");
        payment.setGatewayPaymentId(result.getPaymentId());
        payment.setPaymentUrl(result.getPaymentUrl());
        paymentRepository.save(payment);
        log.info("[PaymentServiceImpl] STEP 3/3 — Payment record updated | dbId={} gatewayPaymentId={} paymentUrl={}",
                payment.getId(), result.getPaymentId(), result.getPaymentUrl());

        log.info("[PaymentServiceImpl] initiatePayment | EXIT — orderId={} checkoutUrl={}",
                command.getOrderId(), result.getPaymentUrl());

        // Return result to caller — PaymentController sends paymentUrl back to order-service
        return result;
    }
}
