package com.hero.bikestore.entity;

/**
 * Represents the lifecycle of an order.
 *
 * AWAITING_PAYMENT → Order placed, stock reserved, payment command sent to payment-service.
 *                    Waiting for Razorpay/mock to confirm or decline.
 *                    Set automatically when placeOrder() publishes payment command.
 *
 * PENDING          → Legacy / manual admin-created orders (no payment saga).
 *                    Also used during fallback if saga is bypassed.
 *
 * CONFIRMED        → Payment captured successfully (auto via saga) OR admin confirmed manually.
 *                    Order sent to warehouse.
 *
 * SHIPPED          → Bike dispatched to the customer.
 * DELIVERED        → Bike received by the customer. Terminal state.
 * CANCELLED        → Payment failed (auto via saga) OR cancelled by customer/admin. Terminal state.
 */
public enum OrderStatus {
    AWAITING_PAYMENT,
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
