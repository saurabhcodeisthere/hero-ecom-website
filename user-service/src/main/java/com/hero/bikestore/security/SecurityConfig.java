package com.hero.bikestore.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomAuthenticationEntryPoint entryPoint,
                                                   CustomAccessDeniedHandler accessDeniedHandler,
                                                   CustomJwtAuthenticationConverter customJwtAuthenticationConverter) throws Exception {


            http
                    .csrf(AbstractHttpConfigurer::disable)

                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/v1/users/**").hasAnyRole("CUSTOMER", "ADMIN")
                    )

                    .oauth2ResourceServer(oauth2 -> oauth2

                            .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter))
                            .accessDeniedHandler(accessDeniedHandler)
                            .authenticationEntryPoint(entryPoint)
                    )

                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(entryPoint))
            ;
            return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Point this to your Keycloak internal URL
        String jwkSetUri = "http://keycloak:8080/realms/hero-ecommerce/protocol/openid-connect/certs";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
