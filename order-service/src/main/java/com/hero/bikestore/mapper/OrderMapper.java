package com.hero.bikestore.mapper;

import com.hero.bikestore.dto.response.AdminOrderResponse;
import com.hero.bikestore.dto.response.OrderItemResponse;
import com.hero.bikestore.dto.response.OrderResponse;
import com.hero.bikestore.entity.DeliveryAddress;
import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderItem;
import com.hero.bikestore.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts Order/OrderItem entities to response DTOs.
 *
 * Two views:
 *   toResponse()      → customer view  (hides userId, userEmail, updatedAt)
 *   toAdminResponse() → admin view     (exposes userId, userEmail, updatedAt)
 *
 * Why manual mapping instead of ModelMapper or MapStruct?
 *   - Explicit control over what fields are exposed to each caller
 *   - No risk of accidentally leaking sensitive fields if entity grows later
 */
@Component
public class OrderMapper {

    // ── Customer view — never exposes who placed the order ───────────────────

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(toItemResponseList(order.getItems()))
                // Only expose the payment URL while the customer still needs to act on it.
                // Once the order moves past AWAITING_PAYMENT the link is dead — returning it
                // would mislead the frontend into thinking a "Pay Now" button is valid.
                // The raw value is intentionally kept in the DB column for audit/support tracing.
                .paymentUrl(order.getStatus() == OrderStatus.AWAITING_PAYMENT
                        ? order.getPaymentUrl()
                        : null)
                .build();
    }

    // ── Admin view — full traceability for operations and support ─────────────

    public AdminOrderResponse toAdminResponse(Order order) {
        return AdminOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())           // visible to admin only
                .userEmail(order.getUserEmail())     // visible to admin only
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())     // visible to admin only
                .items(toItemResponseList(order.getItems()))
                .build();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private List<OrderItemResponse> toItemResponseList(List<OrderItem> items) {
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .bikeId(item.getBikeId())
                .bikeName(item.getBikeName())
                .bikePrice(item.getBikePrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
