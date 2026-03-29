package com.hero.bikestore.dto.payment;

import lombok.*;

/**
 * Event received by order-service from payment.events exchange.
 * Published by payment-service after processing the checkout result.
 *
 * This ends the payment saga step.
 * payment-service says: "Payment for this order succeeded/failed."
 *
 * Routing keys:
 *   "payment.success" → status = SUCCESS → confirm the order
 *   "payment.failed"  → status = FAILED  → restore stock + cancel
 *
 * Mirrors com.hero.bikestore.dto.PaymentResultEvent in payment-service.
 * JSON deserialization uses INFERRED type (method parameter type),
 * so __TypeId__ header pointing to payment-service class is ignored.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentResultEvent {

    private String orderId;           // String of Order.id — used to find the order
    private String orderNumber;       // human-readable reference for logs
    private PaymentStatus status;     // SUCCESS or FAILED

    // Populated on SUCCESS — Razorpay/mock transaction ID
    private String transactionId;

    // Populated on FAILED — shown in cancellation email
    private String failureReason;
}
