package com.hero.bikestore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload sent by the customer when adding a bike to their cart.
 *
 * The userId is NEVER part of this request — it is always extracted from
 * the Keycloak JWT. This prevents a customer from tampering with
 * another customer's cart.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddToCartRequest {

    @NotNull(message = "Bike ID is required")
    @Positive(message = "Bike ID must be a positive number")
    private Long bikeId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Cannot add more than 10 units of the same bike at once")
    private Integer quantity;
}
