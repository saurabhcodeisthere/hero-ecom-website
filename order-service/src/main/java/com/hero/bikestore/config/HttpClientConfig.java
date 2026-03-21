package com.hero.bikestore.config;

import com.hero.bikestore.client.BikeServiceClient;
import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.client.NotificationServiceClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Wires Spring HTTP Interfaces (BikeServiceClient, InventoryServiceClient)
 * to RestClient instances with Eureka load balancing.
 *
 * WHY NOT @LoadBalanced RestClient.Builder?
 * ─────────────────────────────────────────
 * The @LoadBalanced annotation on a RestClient.Builder bean registers a
 * LoadBalancerInterceptor on it globally. Spring Cloud's Eureka client
 * (RestClientEurekaHttpClient) also uses a RestClient internally to register
 * with the discovery server. If it accidentally picks up our @LoadBalanced
 * builder, it tries to load-balance the Eureka registration call itself —
 * but the load balancer cache is empty at startup (chicken-and-egg problem),
 * causing "No instances available for discovery-service" and Eureka
 * registration failure.
 *
 * THE FIX: Inject LoadBalancerInterceptor MANUALLY into only the RestClients
 * we own (bike-service, inventory-service). Eureka's RestClient is left alone.
 *
 * How LoadBalancerInterceptor works per-request:
 *   1. RestClient prepares request to http://bike-service/api/v1/bikes/1
 *   2. Interceptor extracts service name: "bike-service"
 *   3. Calls loadBalancerClient.choose("bike-service") → gets a live instance from Eureka
 *   4. Rewrites URL to real host:port: http://172.18.0.5:8081/api/v1/bikes/1
 *   5. Request proceeds — this happens on EVERY call, so instance changes are picked up automatically
 */
@Configuration
@EnableAsync
public class HttpClientConfig {

    /**
     * HTTP interface proxy for bike-service.
     *
     * Scoped load balancing: only calls to bike-service are load-balanced.
     * Eureka's own RestClient is completely unaffected.
     */
    @Bean
    public BikeServiceClient bikeServiceClient(LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://bike-service")
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(BikeServiceClient.class);
    }

    /**
     * HTTP interface proxy for inventory-service.
     *
     * PATCH is supported natively by RestClient — no extra dependencies needed.
     */
    @Bean
    public InventoryServiceClient inventoryServiceClient(LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://inventory-service")
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(InventoryServiceClient.class);
    }

    /**
     * HTTP interface proxy for notification-service.
     *
     * Called only from NotificationAsyncSender (@Async) — the HTTP call
     * itself is synchronous but the calling thread is a background thread,
     * so order-service's main request thread is never blocked.
     */
    @Bean
    public NotificationServiceClient notificationServiceClient(LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://notification-service")
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(NotificationServiceClient.class);
    }
}
