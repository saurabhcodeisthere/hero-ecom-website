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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controller methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No CSRF needed — stateless REST API with JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless — no HTTP session, every request carries its own JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Docker healthcheck hits this — must be open
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger UI — open for development
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Every other endpoint requires a valid JWT
                        .anyRequest().authenticated())

                // Validate JWT tokens issued by Keycloak
                // Wire our custom converter so Spring Security can read Keycloak roles
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Teaches Spring Security how to read roles from a Keycloak JWT.
     *
     * WHY this is needed:
     * ─────────────────────────────────────────────────────────────────
     * Spring Security by default looks for roles in the "scope" claim:
     *   { "scope": "profile email" }   ← no roles here
     *
     * Keycloak puts roles in a nested object:
     *   { "realm_access": { "roles": ["CUSTOMER", "ADMIN"] } }
     *
     * Without this converter, Spring Security sees zero roles.
     * @PreAuthorize("hasRole('CUSTOMER')") would always return 403.
     *
     * HOW it works:
     * ─────────────────────────────────────────────────────────────────
     * 1. Extract "realm_access" map from the JWT
     * 2. Get the "roles" list from that map
     * 3. Prefix each role with "ROLE_"  ← Spring Security convention
     *    "CUSTOMER" → "ROLE_CUSTOMER"
     *    "ADMIN"    → "ROLE_ADMIN"
     * 4. Wrap each as a SimpleGrantedAuthority
     * 5. Spring Security stores these in the SecurityContext
     * 6. @PreAuthorize("hasRole('CUSTOMER')") now works correctly
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Navigate into the nested realm_access.roles in the Keycloak token
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();   // no roles found — user gets zero authorities
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
