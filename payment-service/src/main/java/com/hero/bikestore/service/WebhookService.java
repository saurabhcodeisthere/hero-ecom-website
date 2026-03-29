package com.hero.bikestore.service;

/**
 * Business contract for processing payment gateway callbacks.
 *
 * In production: called by WebhookController when Razorpay fires an HTTP POST
 * In dev:        called by MockCheckoutController when developer clicks "Pay Now" or "Decline"
 *
 * SRP: Only responsible for updating payment status and publishing the result event.
 *      Does not know about HTTP, RabbitMQ, or checkout pages directly.
 *
 * DIP: MockCheckoutController depends on this interface, not the implementation.
 */
public interface WebhookService {

    /**
     * Marks the payment as SUCCESS, saves transactionId, publishes PaymentResultEvent.
     * order-service will transition the order AWAITING_PAYMENT → CONFIRMED.
     *
     * @param gatewayPaymentId the UUID (mock) or Razorpay orderId (prod) from the checkout URL
     */
    void processSuccess(String gatewayPaymentId);

    /**
     * Marks the payment as FAILED, saves failureReason, publishes PaymentResultEvent.
     * order-service will restore stock and transition the order to CANCELLED.
     *
     * @param gatewayPaymentId the UUID (mock) or Razorpay orderId (prod) from the checkout URL
     * @param failureReason    human-readable reason shown in the cancellation email
     */
    void processFailure(String gatewayPaymentId, String failureReason);
}
