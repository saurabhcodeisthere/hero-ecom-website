package com.hero.bikestore.mapper;

import com.hero.bikestore.dto.response.OrderItemResponse;
import com.hero.bikestore.dto.response.OrderResponse;
import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts Order/OrderItem entities to response DTOs.
 *
 * Why manual mapping instead of ModelMapper or MapStruct?
 *   - Explicit control over what fields are exposed to the client
 *   - Internal fields (userId, version, updatedAt) are intentionally excluded
 *   - No risk of accidentally leaking sensitive fields if entity grows later
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(toItemResponseList(order.getItems()))
                .build();
    }

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
