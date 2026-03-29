package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for bike-service.
 *
 * WHY THIS EXISTS:
 * A transitive dependency (springdoc-openapi or common-exception) pulls in
 * spring-boot-starter-security. This auto-enables HTTP Basic Auth on all
 * endpoints with a randomly generated password — blocking internal
 * service-to-service calls from order-service which send no credentials.
 *
 * bike-service is an internal service — it is never exposed directly to
 * the internet. All external access goes through gateway-service which
 * handles JWT authentication. Internal callers (order-service) are
 * trusted within the Docker network.
 *
 * This config disables the auto-configured Basic Auth and permits all requests.
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
