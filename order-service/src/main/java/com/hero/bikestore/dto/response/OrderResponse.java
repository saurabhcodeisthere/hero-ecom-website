package com.hero.bikestore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    // Present only in the order placement response (AWAITING_PAYMENT).
    // Frontend uses this URL to redirect the customer to the payment page.
    // Null for all other order queries (order is already past payment stage).
    private String paymentUrl;
}
