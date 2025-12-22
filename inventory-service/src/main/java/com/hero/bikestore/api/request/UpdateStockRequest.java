package com.hero.bikestore.api.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStockRequest {

    @NotNull(message = "stockQuantity is required")
    @PositiveOrZero(message = "stockQuantity cannot be negative")
    private Integer stockQuantity;
}
