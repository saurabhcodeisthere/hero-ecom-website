package com.hero.bikestore.dto;

import lombok.*;

/**
 * Result returned by PaymentProcessor.initiate().
 *
 * WHY THIS EXISTS:
 * PaymentProcessor needs to return two things after initiating a payment:
 *   1. paymentId  — identifies this payment attempt (stored in Payment entity)
 *   2. paymentUrl — the checkout URL given to the frontend
 *
 * Dev:  paymentUrl = http://localhost:8086/mock/checkout/{paymentId}
 * Prod: paymentUrl = https://rzp.io/l/abc123  (Razorpay hosted checkout)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentInitiationResult {

    // Internal identifier for this payment — used to look up the record later
    private String paymentId;

    // Frontend redirects the customer to this URL to complete payment
    private String paymentUrl;
}
