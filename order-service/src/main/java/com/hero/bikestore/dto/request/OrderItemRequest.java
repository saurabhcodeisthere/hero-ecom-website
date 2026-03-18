package com.hero.bikestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull(message = "Bike ID is required")
    @Positive(message = "Bike ID must be a positive number")
    private Long bikeId;

    // bikeName and bikePrice removed — fetched from bike-service via Feign client

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Cannot order more than 10 units of the same bike")
    private Integer quantity;
}
