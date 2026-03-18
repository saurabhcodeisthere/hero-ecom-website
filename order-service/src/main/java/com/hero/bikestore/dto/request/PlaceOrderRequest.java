package com.hero.bikestore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress;

    // @Valid cascades validation into each OrderItemRequest inside the list
    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = 10, message = "Cannot order more than 10 different bikes in a single order")
    @Valid
    private List<OrderItemRequest> items;
}
