package com.hero.bikestore.client.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single line item inside the PlaceOrderRequest sent to order-service.
 *
 * Field names (bikeId, quantity) intentionally match order-service's
 * OrderItemRequest fields so JSON serialisation is correct without any
 * custom mapping.
 *
 * order-service fetches the bike name and price itself from bike-service —
 * we do NOT pass the snapshotted name/price here because order-service will
 * re-validate and re-snapshot them anyway.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartOrderItemRequest {

    private Long bikeId;
    private Integer quantity;
}
