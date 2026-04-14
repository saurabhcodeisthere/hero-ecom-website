package com.hero.bikestore.controller;

import com.hero.bikestore.dto.PaymentInitiationResult;
import com.hero.bikestore.dto.ProcessPaymentCommand;
import com.hero.bikestore.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST entry point for payment operations called by order-service.
 *
 * SRP: Only handles HTTP concerns — deserializes request, delegates to
 *      PaymentService, serializes response. No business logic here.
 *
 * DIP: Depends on PaymentService interface, not PaymentServiceImpl.
 *
 * WHY HTTP and not RabbitMQ for initiation?
 * ──────────────────────────────────────────
 * Payment initiation is SYNCHRONOUS — order-service needs the paymentUrl
 * immediately to return it to the frontend so the customer can be redirected
 * to the checkout page.
 *
 * RabbitMQ is fire-and-forget — it cannot return a value back to the caller.
 * HTTP can. So initiation uses HTTP, result (success/failure) uses RabbitMQ.
 *
 *   Initiation  → HTTP  (sync)  → paymentUrl returned immediately ✅
 *   Result      → RabbitMQ (async) → saga completes in background ✅
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Called by order-service when a new order is placed.
     * Creates a Payment record, generates a checkout URL, returns it.
     *
     * order-service includes this URL in the order placement response
     * so the frontend can redirect the customer to the payment page.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResult> initiatePayment(
            @RequestBody ProcessPaymentCommand command) {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[PaymentController] initiatePayment | ENTER");
        log.info("[PaymentController] HTTP POST /payments/initiate received from order-service");
        log.info("[PaymentController] orderId={} orderNumber={} amount={} userEmail={}",
                command.getOrderId(), command.getOrderNumber(), command.getAmount(), command.getUserEmail());
        log.info("[PaymentController] Delegating to PaymentService.initiatePayment()");

        PaymentInitiationResult result = paymentService.initiatePayment(command);

        log.info("[PaymentController] initiatePayment | EXIT — orderId={} paymentUrl={}",
                command.getOrderId(), result.getPaymentUrl());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return ResponseEntity.ok(result);
    }

    /**
     * Called by order-service timeout job when an AWAITING_PAYMENT order expires.
     * Marks the payment as EXPIRED so the mock checkout page rejects any late submission.
     *
     * PATCH — we're updating a single field on an existing resource, not replacing it.
     * Returns 204 No Content — caller needs no data back.
     */
    @PatchMapping("/{orderId}/expire")
    public ResponseEntity<Void> expirePayment(@PathVariable String orderId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[PaymentController] expirePayment | ENTER — orderId={}", orderId);

        paymentService.expirePayment(orderId);

        log.info("[PaymentController] expirePayment | EXIT — orderId={}", orderId);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.noContent().build();
    }
}
