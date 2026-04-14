package com.hero.bikestore.service;

import com.hero.bikestore.dto.PaymentInitiationResult;
import com.hero.bikestore.dto.ProcessPaymentCommand;

/**
 * Business contract for payment operations.
 *
 * SRP: PaymentService owns only payment business logic.
 *      It does not know about RabbitMQ, HTTP, or checkout pages.
 *
 * DIP: PaymentCommandListener and PaymentController depend on this interface,
 *      not the implementation. Allows swapping payment logic without touching callers.
 */
public interface PaymentService {

    /**
     * Initiates a payment for the given order.
     * Creates a Payment record (INITIATED), generates checkout URL,
     * and returns the result to the caller.
     *
     * Returns PaymentInitiationResult containing:
     *   - paymentId  : the gatewayPaymentId (UUID in dev, Razorpay orderId in prod)
     *   - paymentUrl : the checkout URL to redirect the customer to
     */
    PaymentInitiationResult initiatePayment(ProcessPaymentCommand command);

    /**
     * Marks a payment as EXPIRED.
     * Called by order-service timeout job when an AWAITING_PAYMENT order exceeds
     * the cancellation threshold and the customer never completed checkout.
     *
     * Rules:
     *   - Only INITIATED payments are expired. SUCCESS/FAILED/EXPIRED are skipped.
     *   - If no payment record found for orderId, logs a warning and does nothing.
     *
     * @param orderId the order ID whose payment should be expired
     */
    void expirePayment(String orderId);
}
