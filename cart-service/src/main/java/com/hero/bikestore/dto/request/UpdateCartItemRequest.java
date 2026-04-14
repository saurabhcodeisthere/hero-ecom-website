package com.hero.bikestore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload sent by the customer when changing the quantity of a cart item.
 *
 * To remove an item completely, the customer should call DELETE /cart/items/{id}.
 * Setting quantity to 0 is intentionally disallowed here — it would create
 * ambiguity between "zero units wanted" and "item removed".
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateCartItemRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Cannot have more than 10 units of the same bike in the cart")
    private Integer quantity;
}
