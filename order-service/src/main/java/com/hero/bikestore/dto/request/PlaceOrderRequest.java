package com.hero.bikestore.dto.request;

import com.hero.bikestore.entity.DeliveryAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlaceOrderRequest {

    // @Valid cascades Bean Validation into the DeliveryAddress fields
    // (fullName, phone, streetLine1, city, state, pincode all have @NotBlank/@Pattern)
    @NotNull(message = "Shipping address is required")
    @Valid
    private DeliveryAddress shippingAddress;

    // @Valid cascades validation into each OrderItemRequest inside the list
    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = 10, message = "Cannot order more than 10 different bikes in a single order")
    @Valid
    private List<OrderItemRequest> items;
}
