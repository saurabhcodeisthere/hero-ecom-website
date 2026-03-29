package com.hero.bikestore.dto.payment;

/**
 * Payment result states received from payment-service.
 *
 * order-service only cares about the terminal states:
 *   SUCCESS → confirm the order
 *   FAILED  → restore stock + cancel the order
 *
 * INITIATED is not relevant here — order-service never receives it.
 * Mirrors com.hero.bikestore.enums.PaymentStatus in payment-service.
 * JSON deserialization matches by value name, not by class name.
 */
public enum PaymentStatus {
    SUCCESS,
    FAILED
}
