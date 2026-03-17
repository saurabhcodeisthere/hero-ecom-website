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
                .route("bike-service", r -> r
                        .path("/api/bikes/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("lb://bike-service"))        // lb:// = Eureka load-balanced lookup

                .route("inventory-service", r -> r
                        .path("/api/v1/inventories/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("lb://inventory-service"))   // resolves via Eureka registry

                .route("user-service", r -> r
                        .path("/api/v1/users/**", "/api/v1/admin/users/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("lb://user-service"))        // no hardcoded port — Eureka handles it
                .build();
    }
}
