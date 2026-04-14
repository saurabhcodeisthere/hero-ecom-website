package com.hero.bikestore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Delivery address sent by the customer at checkout time.
 *
 * This is a local DTO — cart-service does NOT import any class from order-service.
 * Field names intentionally match order-service's DeliveryAddress entity so that
 * when this object is serialised to JSON and forwarded to order-service, the
 * JSON keys align with what order-service's PlaceOrderRequest expects.
 *
 * Validation here gives fast, clear error messages at the cart boundary —
 * before the request ever leaves this service.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeliveryAddressDto {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter a valid 10-digit Indian mobile number starting with 6–9"
    )
    private String phone;

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    private String streetLine1;

    // Optional — landmark or area
    @Size(max = 200, message = "Street line 2 must not exceed 200 characters")
    private String streetLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(
            regexp = "^[1-9][0-9]{5}$",
            message = "Enter a valid 6-digit Indian pincode"
    )
    private String pincode;

    /**
     * Converts a user-service address response into a DeliveryAddressDto
     * so checkout can use it identically to an inline address.
     */
    public static DeliveryAddressDto from(com.hero.bikestore.client.response.UserAddressClientResponse.UserAddressData a) {
        DeliveryAddressDto dto = new DeliveryAddressDto();
        dto.setFullName(a.getFullName());
        dto.setPhone(a.getPhone());
        dto.setStreetLine1(a.getStreetLine1());
        dto.setStreetLine2(a.getStreetLine2());
        dto.setCity(a.getCity());
        dto.setState(a.getState());
        dto.setPincode(a.getPincode());
        return dto;
    }
}
