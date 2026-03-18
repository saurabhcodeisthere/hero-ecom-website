package com.hero.bikestore.entity;

/**
 * Represents the lifecycle of an order.
 *
 * PENDING   → Order placed, awaiting admin review.
 * CONFIRMED → Admin verified the order, sent to warehouse.
 * SHIPPED   → Bike dispatched to the customer.
 * DELIVERED → Bike received by the customer. Terminal state.
 * CANCELLED → Order cancelled by customer (PENDING/CONFIRMED only)
 *             or by admin at any stage. Terminal state.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
