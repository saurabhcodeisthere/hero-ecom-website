package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for notification-service.
 *
 * WHY permit all?
 * ────────────────
 * notification-service is an INTERNAL service — it is never exposed to
 * the internet. It sits behind the API gateway and is only reachable by
 * other services on the Docker / Kubernetes internal network.
 *
 * The common-exception library pulls in spring-boot-starter-security
 * (needed for AccessDeniedException handling in GlobalExceptionHandler).
 * Without this config, Spring Boot's auto-configuration would lock down
 * all endpoints with HTTP Basic Auth — causing 401 on every call from
 * order-service.
 *
 * Network-level isolation (Docker network / K8s namespace) is the
 * security boundary for this service. No JWT or Basic Auth needed here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
