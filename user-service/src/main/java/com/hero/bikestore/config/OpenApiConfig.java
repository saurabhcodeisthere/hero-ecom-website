package com.hero.bikestore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hero User Service API")
                        .description("""
                                Manages Hero platform user accounts backed by Keycloak for identity.

                                **User endpoints** (`/api/v1/users/**`) — require a valid JWT with \
                                CUSTOMER or ADMIN role. Used to fetch profile information.
                                **Admin endpoints** (`/api/v1/admin/users/**`) — require JWT with \
                                ADMIN role. Used to block or activate user accounts.

                                Users are auto-created on first login via Keycloak JWT claims.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Hero Ecom Platform")
                                .email("platform@hero.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your Keycloak access token here (without the 'Bearer ' prefix)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
