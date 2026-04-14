package com.hero.bikestore.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * The fields cart-service reads from the order-service response
 * after a successful order placement.
 *
 * cart-service only needs the bare minimum to build a CheckoutResponse
 * for the customer. Everything else (items, address details) is already
 * known at checkout time and need not be re-parsed.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderClientResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;

    // Redirect URL returned by payment-service via order-service.
    // The customer MUST be sent here to complete payment.
    // Null if payment initiation failed or was not required.
    private String paymentUrl;
}
