package com.hero.bikestore.dto;

import com.hero.bikestore.enums.PaymentStatus;
import lombok.*;

/**
 * Event published by payment-service to payment.events exchange.
 * Consumed by PaymentReplyListener in order-service.
 *
 * This is the END of the payment saga step.
 * payment-service says: "Payment for this order succeeded/failed."
 *
 * Routing keys:
 *   SUCCESS → "payment.success"
 *   FAILED  → "payment.failed"
 *
 * order-service uses this to:
 *   SUCCESS → transition order AWAITING_PAYMENT → CONFIRMED
 *   FAILED  → restore stock + transition to CANCELLED
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentResultEvent {

    private String orderId;
    private String orderNumber;
    private PaymentStatus status;

    // Populated on SUCCESS — Razorpay payment ID or mock transaction ID
    private String transactionId;

    // Populated on FAILED — reason shown in cancellation email
    private String failureReason;
}
