package com.hero.bikestore.config;

import com.hero.bikestore.client.BikeServiceClient;
import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.client.OrderServiceClient;
import com.hero.bikestore.client.UserServiceClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Wires the three HTTP Interface clients with Eureka load-balanced RestClients.
 *
 * WHY manual LoadBalancerInterceptor instead of @LoadBalanced RestClient.Builder?
 * ─────────────────────────────────────────────────────────────────────────────────
 * @LoadBalanced on a RestClient.Builder registers the interceptor globally.
 * Spring Cloud's Eureka client also uses a RestClient internally to register
 * with discovery-service. If it picks up our interceptor it tries to
 * load-balance the Eureka registration call itself — but the load balancer
 * cache is empty at startup → "No instances available for discovery-service".
 *
 * THE FIX: Inject LoadBalancerInterceptor MANUALLY into only the three
 * RestClients we own. Eureka's own RestClient is untouched.
 */
@Configuration
public class HttpClientConfig {

    /**
     * HTTP Interface proxy for bike-service.
     * Used at add-to-cart time: verify bike exists, snapshot name + price.
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
     * HTTP Interface proxy for inventory-service.
     * Used at add-to-cart time: soft stock check — reject add if out of stock.
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
     * HTTP Interface proxy for order-service.
     * Used at checkout: forwards cart items + delivery address to create an order.
     * The customer's JWT is passed as a request header so order-service can
     * identify the customer without trusting any value in the request body.
     */
    @Bean
    public OrderServiceClient orderServiceClient(LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://order-service")
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OrderServiceClient.class);
    }

    /**
     * HTTP Interface proxy for user-service.
     * Used at checkout when customer provides addressId instead of inline address.
     */
    @Bean
    public UserServiceClient userServiceClient(LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://user-service")
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(UserServiceClient.class);
    }
}
