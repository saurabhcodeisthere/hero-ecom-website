package com.hero.bikestore.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Only the fields cart-service needs from bike-service.
 *
 * Consumer-driven contract — cart-service defines what it reads,
 * not what bike-service returns. If bike-service adds 20 new fields
 * tomorrow, this class remains unchanged.
 */
@Getter
@Setter
@NoArgsConstructor
public class BikeClientResponse {

    private Long id;
    private String modelName;
    private BigDecimal price;

    // true = bike is available for purchase; false = discontinued or unlisted
    private boolean active;
}
