package com.hero.bikestore.client;

import com.hero.bikestore.client.response.InventoryClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Spring HTTP Interface client for inventory-service.
 *
 * Called at add-to-cart time to do a SOFT stock check.
 * If stockQuantity == 0 or the inventory record is inactive,
 * the add is rejected with a clear message to the customer.
 *
 * This is a SOFT check — stock is not reserved here.
 * The hard reservation (decrement + lock) happens inside
 * order-service at checkout time. A race condition between
 * add-to-cart and checkout is handled by order-service's
 * InsufficientStockException.
 */
@HttpExchange
public interface InventoryServiceClient {

    @GetExchange("/api/v1/inventories/bike/{bikeId}")
    InventoryClientResponse getInventoryByBikeId(@PathVariable Long bikeId);
}
