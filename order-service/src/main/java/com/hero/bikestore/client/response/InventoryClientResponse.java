package com.hero.bikestore.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents only the fields order-service needs from inventory-service.
 */
@Getter
@Setter
@NoArgsConstructor
public class InventoryClientResponse {

    private Long id;
    private Long bikeId;
    private Double price;
    private Integer stockQuantity;
    private boolean active;
}
