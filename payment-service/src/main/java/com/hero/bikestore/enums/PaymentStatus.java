package com.hero.bikestore.enums;

/**
 * Lifecycle of a payment record.
 *
 * INITIATED → Payment created, waiting for customer to complete checkout.
 * SUCCESS   → Payment captured successfully. Terminal state — triggers order confirmation.
 * FAILED    → Payment declined or errored. Terminal state — triggers order cancellation + stock restore.
 */
public enum PaymentStatus {
    INITIATED,
    SUCCESS,
    FAILED,

    /**
     * Set by order-service timeout job when an AWAITING_PAYMENT order exceeds
     * the cancellation threshold and no payment was received.
     * Prevents the mock checkout page from accepting payment after the order is cancelled.
     */
    EXPIRED
}
