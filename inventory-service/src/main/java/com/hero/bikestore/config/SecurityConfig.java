package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for inventory-service.
 *
 * WHY THIS EXISTS:
 * A transitive dependency pulls in spring-boot-starter-security, which
 * auto-enables HTTP Basic Auth with a randomly generated password.
 * This blocks internal service-to-service calls from order-service
 * which send no credentials.
 *
 * inventory-service is internal — never exposed directly to the internet.
 * All external access goes through gateway-service. Internal callers
 * (order-service) are trusted within the Docker network.
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
