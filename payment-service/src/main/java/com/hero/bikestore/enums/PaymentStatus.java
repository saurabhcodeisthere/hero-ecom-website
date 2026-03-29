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
    FAILED
}
