package com.hero.bikestore.client;

import com.hero.bikestore.client.response.BikeClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Spring HTTP Interface client for bike-service.
 *
 * Called at add-to-cart time to:
 *   1. Verify the bike exists (404 → stop, tell the customer)
 *   2. Verify the bike is active (inactive → reject add)
 *   3. Snapshot the name and price for the CartItem row
 *
 * Base URL (http://bike-service) is resolved via Eureka by the
 * LoadBalancerInterceptor wired in HttpClientConfig.
 */
@HttpExchange
public interface BikeServiceClient {

    @GetExchange("/api/v1/bikes/{id}")
    BikeClientResponse getBikeById(@PathVariable Long id);
}
