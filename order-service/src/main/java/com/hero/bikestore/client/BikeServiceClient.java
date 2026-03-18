package com.hero.bikestore.client;

import com.hero.bikestore.client.response.BikeClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Spring HTTP Interface client for bike-service.
 *
 * @HttpExchange marks this as an HTTP Interface — Spring generates the
 * implementation at startup via HttpServiceProxyFactory (wired in HttpClientConfig).
 *
 * The base URL (http://bike-service) is set in HttpClientConfig.
 * Spring Cloud LoadBalancer intercepts that logical name and resolves it
 * to a real host:port using Eureka — no hardcoded URLs anywhere.
 *
 * Only declares the ONE endpoint order-service needs — not the full bike-service API.
 */
@HttpExchange
public interface BikeServiceClient {

    @GetExchange("/api/v1/bikes/{id}")
    BikeClientResponse getBikeById(@PathVariable Long id);
}
