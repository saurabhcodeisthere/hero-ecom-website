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
 * Orchestrates the payment initiation and expiry flows.
 *
 * SRP: This class owns only the payment business logic.
 *      - Does not know about RabbitMQ (depends on PaymentProcessor interface)
 *      - Does not know about Razorpay (depends on PaymentProcessor interface)
 *      - Does not know about HTTP (no controller logic here)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessor  paymentProcessor;

    @Override
    @Transactional
    public PaymentInitiationResult initiatePayment(ProcessPaymentCommand command) {
        log.info("[PaymentServiceImpl] initiatePayment | ENTER");
        log.info("[PaymentServiceImpl] orderId={} orderNumber={} amount={} userEmail={}",
                command.getOrderId(), command.getOrderNumber(), command.getAmount(), command.getUserEmail());

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

        log.info("[PaymentServiceImpl] STEP 2/3 — Calling {} to generate checkout URL",
                paymentProcessor.getClass().getSimpleName());
        PaymentInitiationResult result = paymentProcessor.initiate(command);
        log.info("[PaymentServiceImpl] STEP 2/3 — Checkout URL generated | gatewayPaymentId={} paymentUrl={}",
                result.getPaymentId(), result.getPaymentUrl());

        log.info("[PaymentServiceImpl] STEP 3/3 — Updating Payment record with gatewayPaymentId and paymentUrl");
        payment.setGatewayPaymentId(result.getPaymentId());
        payment.setPaymentUrl(result.getPaymentUrl());
        paymentRepository.save(payment);
        log.info("[PaymentServiceImpl] STEP 3/3 — Payment record updated | dbId={} gatewayPaymentId={} paymentUrl={}",
                payment.getId(), result.getPaymentId(), result.getPaymentUrl());

        log.info("[PaymentServiceImpl] initiatePayment | EXIT — orderId={} checkoutUrl={}",
                command.getOrderId(), result.getPaymentUrl());
        return result;
    }

    @Override
    @Transactional
    public void expirePayment(String orderId) {
        log.info("[PaymentServiceImpl] expirePayment | ENTER — orderId={}", orderId);

        paymentRepository.findByOrderId(orderId).ifPresentOrElse(
            payment -> {
                if (payment.getStatus() != PaymentStatus.INITIATED) {
                    // Payment already resolved (SUCCESS / FAILED / EXPIRED).
                    // This happens if customer paid just before the timeout job ran.
                    log.info("[PaymentServiceImpl] expirePayment — skipping, already status={} | orderId={}",
                            payment.getStatus(), orderId);
                    return;
                }
                payment.setStatus(PaymentStatus.EXPIRED);
                paymentRepository.save(payment);
                log.info("[PaymentServiceImpl] expirePayment — payment set to EXPIRED | orderId={} gatewayPaymentId={}",
                        orderId, payment.getGatewayPaymentId());
            },
            () -> log.warn("[PaymentServiceImpl] expirePayment — no payment record found | orderId={}", orderId)
        );

        log.info("[PaymentServiceImpl] expirePayment | EXIT — orderId={}", orderId);
    }
}
