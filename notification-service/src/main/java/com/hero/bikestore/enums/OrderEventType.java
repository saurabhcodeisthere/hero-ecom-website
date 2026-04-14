package com.hero.bikestore.enums;

/**
 * All order lifecycle events that trigger a notification.
 *
 * Each value maps to exactly one handler via the Strategy pattern:
 *   ORDER_PLACED    → OrderPlacedHandler
 *   ORDER_CONFIRMED → OrderConfirmedHandler
 *   ORDER_SHIPPED   → OrderShippedHandler
 *   ORDER_DELIVERED → OrderDeliveredHandler
 *   ORDER_CANCELLED → OrderCancelledHandler
 */
public enum OrderEventType {
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,

    /**
     * Sent by order-service timeout job when an AWAITING_PAYMENT order crosses
     * the warning threshold but is still within the grace period.
     * Routing key: order.payment.expiry.warning — matches binding "order.#" ✅
     * Handled by: PaymentExpiryWarningHandler
     */
    ORDER_PAYMENT_EXPIRY_WARNING
}
