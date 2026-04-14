package com.hero.bikestore.dto.response;

import com.hero.bikestore.entity.DeliveryAddress;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin view of an order — includes userId, userEmail, updatedAt.
 *
 * Intentionally separate from OrderResponse (customer view):
 *   - Customer must never see other customers' userId/email
 *   - Admin needs full traceability for support and operations
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderResponse {

    private Long orderId;
    private String orderNumber;

    // Exposed to admin only — customer response never includes these
    private String userId;
    private String userEmail;

    private String status;
    private BigDecimal totalAmount;

    // Structured address — admin can see full address including phone number
    private DeliveryAddress shippingAddress;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
}
