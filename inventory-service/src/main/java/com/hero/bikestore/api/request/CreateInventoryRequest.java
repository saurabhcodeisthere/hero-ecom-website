package com.hero.bikestore.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInventoryRequest {

    @NotNull(message = "bikeId is required")
    private Long bikeId;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private Double price;

    @NotNull(message = "stockQuantity is required")
    @Positive(message = "stockQuantity must be positive")
    private Integer stockQuantity;
}
