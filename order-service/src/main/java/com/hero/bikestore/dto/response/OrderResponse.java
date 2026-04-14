package com.hero.bikestore.dto.response;

import com.hero.bikestore.entity.DeliveryAddress;
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

    // Structured address — frontend can render individual fields (city, pincode etc.)
    private DeliveryAddress shippingAddress;

    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    // Only present when status == AWAITING_PAYMENT.
    // Frontend uses this URL to redirect the customer to the payment page.
    // Null for all other statuses — payment link is no longer valid once the
    // order moves past the AWAITING_PAYMENT stage.
    private String paymentUrl;
}
