package com.hero.bikestore.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Command published by order-service to payment.command.queue.
 * Consumed by PaymentCommandListener in payment-service.
 *
 * This is the START of the payment saga step.
 * order-service says: "Please process a payment for this order."
 *
 * Fields match exactly what payment-service needs to:
 *   1. Create a Payment record in paymentdb
 *   2. Call Razorpay/mock to get the checkout URL
 *   3. Return the URL to the frontend
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProcessPaymentCommand {

    private String orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String userEmail;
    private String userName;
}
