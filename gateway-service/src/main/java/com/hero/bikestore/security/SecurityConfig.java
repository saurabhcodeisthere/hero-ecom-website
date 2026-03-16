package com.hero.bikestore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthenticationEntryPoint authEntryPoint;
    private final GatewayAccessDeniedHandler      accessDeniedHandler;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Handles: bearer token present but expired / invalid / malformed → 401
                        .authenticationEntryPoint(authEntryPoint)
                        .jwt(Customizer.withDefaults())
                )
                .exceptionHandling(ex -> ex
                        // Handles: no bearer token provided at all → 401
                        .authenticationEntryPoint(authEntryPoint)
                        // Handles: valid token but insufficient role → 403
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        String jwkSetUri = "http://keycloak:8080/realms/hero-ecommerce/protocol/openid-connect/certs";
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
