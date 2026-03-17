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
    public OpenAPI bikeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hero Bike Service API")
                        .description("""
                                Manages the Hero bike catalog — browse, search, filter bikes and \
                                admin operations to add/update/activate/deactivate listings.

                                **Public endpoints** — require a valid JWT (Bearer token).
                                **Admin endpoints** (`/api/v1/admin/**`) — require JWT with ADMIN role.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Hero Ecom Platform")
                                .email("platform@hero.com")))
                // Register the JWT Bearer scheme once — controllers reference it via @SecurityRequirement
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your Keycloak access token here (without the 'Bearer ' prefix)")))
                // Apply JWT requirement globally to every endpoint shown in this doc
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
