package com.hero.bikestore.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Only the fields cart-service needs from inventory-service.
 *
 * stockQuantity — used at add-to-cart time to give the customer an early
 * warning if the item is out of stock, avoiding a frustrating checkout failure later.
 *
 * NOTE: This is a SOFT check only.
 * Stock is not reserved at this point — another customer could buy the last
 * unit between add-to-cart and checkout. The hard stock reservation happens
 * inside order-service at checkout time.
 */
@Getter
@Setter
@NoArgsConstructor
public class InventoryClientResponse {

    private Long id;
    private Long bikeId;
    private Integer stockQuantity;
    private boolean active;
}
