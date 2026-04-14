package com.hero.bikestore.enums;

/**
 * All order lifecycle events that trigger a notification.
 * Mirrors the enum in notification-service.
 *
 * Sent inside OrderNotificationEvent.type when calling
 * notification-service via NotificationServiceClient.
 */
public enum OrderEventType {
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,

    /**
     * Sent by PaymentTimeoutJob when an AWAITING_PAYMENT order crosses the warning
     * threshold but is still within the grace period (between warningMinutes and cancelMinutes).
     *
     * Routing key: order.payment.expiry.warning
     * Matches notification-service binding: order.# ✅
     *
     * Triggers a "Your payment link is about to expire" email via PaymentExpiryWarningHandler.
     */
    ORDER_PAYMENT_EXPIRY_WARNING
}
