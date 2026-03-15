package com.hero.bikestore.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("bike-service",r->r
                        .path("/api/bikes/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("http://bike-service:8081"))

                .route("inventory-service",r->r
                        .path("/api/v1/inventories/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("http://inventory-service:8082"))

                .route("user-service",r->r
                        .path("/api/v1/users/**", "/api/v1/admin/users/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("http://user-service:8083"))
                .build();
    }
}
