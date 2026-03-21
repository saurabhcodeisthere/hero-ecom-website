package com.hero.bikestore.dto;

import com.hero.bikestore.enums.OrderEventType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Inbound event payload sent from order-service to notification-service.
 *
 * One event structure covers all order lifecycle types.
 * The 'type' field drives which handler processes the event.
 *
 * Optional fields (trackingId, cancellationReason, estimatedDelivery)
 * are nested inside metadata — present only when relevant to the event type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotificationEvent {

    @NotNull(message = "Event type is required")
    private OrderEventType type;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotBlank(message = "User email is required")
    @Email(message = "User email must be valid")
    private String userEmail;

    private String userName;

    private Instant occurredAt;

    private List<OrderItemEvent> items;

    private BigDecimal totalAmount;

    private String shippingAddress;

    // Optional fields per event type — null when not applicable
    private EventMetadata metadata;

    // ─────────────────────────────────────────────────────────────────
    // Nested classes — kept here as static inners for cohesion
    // ─────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private String bikeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {

        // Present for ORDER_SHIPPED
        private String trackingId;

        // Present for ORDER_SHIPPED and ORDER_CONFIRMED
        private String estimatedDelivery;

        // Present for ORDER_CANCELLED
        private String cancellationReason;
    }
}
