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
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hero Inventory Service API")
                        .description("""
                                Manages stock levels and pricing for Hero bikes.

                                **Public endpoints** — check stock availability by bike ID.
                                **Admin endpoints** (`/api/v1/admin/**`) — create inventory records \
                                and update stock quantities. Require JWT with ADMIN role.

                                Each inventory record is linked to a bike via `bikeId` (no hard FK — \
                                decoupled microservice design).
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
