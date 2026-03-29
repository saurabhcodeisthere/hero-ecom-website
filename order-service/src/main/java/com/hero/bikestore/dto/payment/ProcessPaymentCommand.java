package com.hero.bikestore.dto.payment;

import lombok.*;

import java.math.BigDecimal;

/**
 * Command published by order-service to payment.command.queue.
 * Consumed by PaymentCommandListener in payment-service.
 *
 * This starts the payment saga step.
 * order-service says: "Please process payment for this order."
 *
 * Fields are exactly what payment-service needs to:
 *   1. Create a Payment record in paymentdb
 *   2. Call Razorpay/mock to get the checkout URL
 *
 * Mirrors com.hero.bikestore.dto.ProcessPaymentCommand in payment-service.
 * JSON structure must match — class name and package do not need to.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProcessPaymentCommand {

    private String orderId;        // String form of Order.id (Long)
    private String orderNumber;    // e.g. ORD-20260326-A3F9C1B2
    private BigDecimal amount;     // total amount to charge
    private String userEmail;      // for payment receipt email
    private String userName;       // for payment receipt personalisation
}
