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
    ORDER_CANCELLED
}
