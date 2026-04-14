package com.hero.bikestore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Secures all cart-service endpoints with Keycloak JWT validation.
 *
 * Every cart operation requires the customer to be authenticated —
 * there is no public cart API.
 *
 * The JwtAuthenticationConverter teaches Spring Security to read
 * roles from Keycloak's "realm_access.roles" claim instead of the
 * default "scope" claim (which Keycloak does not use for roles).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controller methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No CSRF — stateless REST API protected by JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless — no HTTP session; every request carries its own JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Docker healthcheck — must be open (no JWT)
                        .requestMatchers("/actuator/**").permitAll()
                        // Every cart endpoint requires a valid JWT
                        .anyRequest().authenticated())

                // Validate tokens issued by Keycloak
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Reads Keycloak roles from realm_access.roles and maps them to
     * Spring Security GrantedAuthorities with the "ROLE_" prefix.
     *
     * Without this, @PreAuthorize("hasRole('CUSTOMER')") always returns 403
     * because Spring Security finds no roles in the JWT.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            // "CUSTOMER" → SimpleGrantedAuthority("ROLE_CUSTOMER")
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return converter;
    }
}
