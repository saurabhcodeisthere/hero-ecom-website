package com.hero.bikestore.service;

/**
 * Contract for payment timeout operations.
 *
 * SOLID design:
 *
 * SRP — Only defines timeout-related behaviour. Nothing else.
 *
 * ISP — Thin interface. PaymentTimeoutJob calls these two methods and
 *       knows nothing about repositories, HTTP clients, or RabbitMQ.
 *
 * DIP — PaymentTimeoutJob depends on this interface, not TimeoutServiceImpl.
 *       Spring injects the implementation. Unit tests can inject a mock.
 *
 * OCP — Adding new timeout behaviour (e.g. sendFinalWarning at 13 min):
 *         1. Add method here
 *         2. Implement in TimeoutServiceImpl
 *         3. Call from PaymentTimeoutJob
 *         Zero changes to existing methods.
 */
public interface TimeoutService {

    /**
     * Finds AWAITING_PAYMENT orders that have crossed the warning threshold
     * but NOT yet the cancellation threshold, and whose warning email has not
     * yet been sent (paymentReminderSentAt IS NULL).
     *
     * Per matching order:
     *   - Marks paymentReminderSentAt = now() to prevent duplicate emails
     *   - Publishes ORDER_PAYMENT_EXPIRY_WARNING event to RabbitMQ
     */
    void sendExpiryWarnings();

    /**
     * Finds AWAITING_PAYMENT orders that have crossed the cancellation threshold.
     *
     * Per matching order:
     *   - Restores stock for every item (best-effort — failures published to DLQ)
     *   - Transitions status AWAITING_PAYMENT → CANCELLED
     *   - Calls payment-service to mark payment as EXPIRED (best-effort)
     *   - Publishes ORDER_CANCELLED notification to RabbitMQ
     */
    void cancelExpiredOrders();
}
