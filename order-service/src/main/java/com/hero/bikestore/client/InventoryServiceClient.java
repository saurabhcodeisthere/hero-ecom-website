package com.hero.bikestore.client;

import com.hero.bikestore.client.response.InventoryClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

/**
 * Spring HTTP Interface client for inventory-service.
 *
 * PATCH is supported natively by RestClient (unlike Java's old HttpURLConnection
 * which required the feign-hc5 workaround). No extra dependencies needed.
 *
 * The base URL and load balancing are configured in HttpClientConfig.
 */
@HttpExchange
public interface InventoryServiceClient {

    // Check current stock for a bike before placing an order
    @GetExchange("/api/v1/inventories/bike/{bikeId}")
    InventoryClientResponse getInventoryByBikeId(@PathVariable Long bikeId);

    // Reduce stock when an order is placed — inventory-service validates & decrements atomically
    @PatchExchange("/api/v1/inventories/bike/{bikeId}/reduce")
    InventoryClientResponse reduceStock(@PathVariable Long bikeId, @RequestParam Integer quantity);

    // Restore stock when an order is cancelled
    @PatchExchange("/api/v1/inventories/bike/{bikeId}/restore")
    InventoryClientResponse restoreStock(@PathVariable Long bikeId, @RequestParam Integer quantity);
}
