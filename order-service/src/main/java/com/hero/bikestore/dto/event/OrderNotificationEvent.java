package com.hero.bikestore.dto.event;

import com.hero.bikestore.enums.OrderEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Outbound event payload sent from order-service to notification-service.
 *
 * Built inside OrderService.notify() after every order status change,
 * then fired asynchronously via NotificationAsyncSender.
 *
 * Must match the structure expected by notification-service's
 * OrderNotificationEvent — Jackson serializes/deserializes using field names.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotificationEvent {

    private OrderEventType type;
    private Long orderId;
    private String orderNumber;
    private String userEmail;
    private String userName;
    private Instant occurredAt;
    private List<OrderItemEvent> items;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private EventMetadata metadata;

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
        private String trackingId;
        private String estimatedDelivery;
        private String cancellationReason;
    }
}
