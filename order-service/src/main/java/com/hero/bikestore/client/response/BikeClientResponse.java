package com.hero.bikestore.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents only the fields order-service needs from bike-service.
 * We do NOT import bike-service's full BikeResponse — each service
 * defines its own view of external data (consumer-driven contract).
 */
@Getter
@Setter
@NoArgsConstructor
public class BikeClientResponse {

    private Long id;
    private String modelName;
    private BigDecimal price;
}
