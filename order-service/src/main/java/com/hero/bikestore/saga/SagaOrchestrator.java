package com.hero.bikestore.saga;

import com.hero.bikestore.dto.payment.PaymentResultEvent;

/**
 * DIP — business contract for handling payment saga outcomes.
 *
 * PaymentReplyListener depends on this interface, not the implementation.
 * If the saga logic changes (e.g. two-phase commit), swap the implementation
 * without touching the listener.
 *
 * SRP: Only owns saga coordination logic — what to do when payment succeeds or fails.
 */
public interface SagaOrchestrator {

    /**
     * Payment captured successfully.
     * Transitions order AWAITING_PAYMENT → CONFIRMED.
     * Publishes order.confirmed notification.
     */
    void handlePaymentSuccess(PaymentResultEvent event);

    /**
     * Payment declined or failed.
     * Restores stock for every item in the order (compensation).
     * Transitions order AWAITING_PAYMENT → CANCELLED.
     * Publishes order.cancelled notification.
     */
    void handlePaymentFailure(PaymentResultEvent event);
}
