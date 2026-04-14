package com.hero.bikestore.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload sent by the customer when they click "Checkout".
 *
 * Two modes — exactly one must be provided:
 *
 * Mode 1 — saved address (preferred):
 *   { "addressId": 3 }
 *   cart-service calls user-service to fetch the full address by ID.
 *   The customer never has to type the address again after saving it once.
 *
 * Mode 2 — inline address (backward compatible, Postman still works):
 *   { "shippingAddress": { "fullName": ..., "phone": ..., ... } }
 *   Used when the customer wants to deliver to a one-time address
 *   without saving it to their profile.
 *
 * If both are provided, addressId takes priority.
 * If neither is provided, a 400 error is returned.
 */
@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {

    // Option 1 — use a saved address from user-service
    private Long addressId;

    // Option 2 — provide the address inline (no saving)
    @Valid
    private DeliveryAddressDto shippingAddress;
}
