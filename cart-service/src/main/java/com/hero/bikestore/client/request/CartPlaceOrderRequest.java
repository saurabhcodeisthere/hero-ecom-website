package com.hero.bikestore.client.request;

import com.hero.bikestore.dto.request.DeliveryAddressDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The exact request body that cart-service sends to order-service's
 * POST /api/v1/orders endpoint.
 *
 * Field names must match order-service's PlaceOrderRequest exactly:
 *   - "shippingAddress" → maps to DeliveryAddress @Embedded in order-service
 *   - "items"           → maps to List<OrderItemRequest> in order-service
 *
 * The userId is NOT included here — order-service reads it from the JWT
 * (Authorization: Bearer <token>) which cart-service forwards in the header.
 * This design ensures order-service always trusts the Keycloak-signed token,
 * never a value passed in the request body.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartPlaceOrderRequest {

    // Forwarded from the customer's CheckoutRequest — never modified by cart-service
    private DeliveryAddressDto shippingAddress;

    // Built from the customer's saved CartItems — bikeId + quantity only
    private List<CartOrderItemRequest> items;
}
