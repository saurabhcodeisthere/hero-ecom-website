package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for payment-service.
 *
 * WHY THIS EXISTS:
 * common-exception module pulls in spring-boot-starter-security as a transitive
 * dependency. This auto-enables Basic Auth on all endpoints, which would block:
 *   - Razorpay webhook calls (external — no JWT)
 *   - Internal RabbitMQ-triggered flows
 *   - Mock checkout endpoints (dev testing)
 *
 * This config explicitly permits all requests.
 * Webhook signature verification (HMAC-SHA256) handles security at the application level.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
