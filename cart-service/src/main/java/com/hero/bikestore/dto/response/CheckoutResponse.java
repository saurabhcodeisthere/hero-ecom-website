package com.hero.bikestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response returned to the customer after a successful checkout.
 *
 * The paymentUrl is the most critical field — the frontend must redirect
 * the customer to this URL so they can complete payment.
 * It comes directly from order-service → payment-service and is valid
 * only for a limited time (defined by the payment timeout window).
 *
 * The cart is cleared server-side before this response is sent.
 * If the customer wants to re-order, they must add items again.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;

    // Redirect the customer here to complete payment.
    // Only present when status == AWAITING_PAYMENT.
    // Null if payment initiation failed or was not required.
    private String paymentUrl;

    private String message;
}
