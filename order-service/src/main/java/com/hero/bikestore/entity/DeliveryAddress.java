package com.hero.bikestore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Structured delivery address embedded directly into the orders table.
 *
 * @Embeddable means this class has NO table of its own — its fields are
 * flattened as columns into whichever @Entity uses @Embedded on it.
 *
 * This gives us:
 *   - Structured, validatable fields (pincode format, phone format)
 *   - Individual columns for filtering (city, state) in admin queries
 *   - A toDisplayString() helper for notification emails
 *
 * IMPORTANT — this is intentionally a SNAPSHOT.
 * When an order is placed the full address is copied here.
 * If the customer later changes their profile address, past orders
 * are unaffected — each order permanently stores what was entered at checkout.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

    // Who receives the delivery — may differ from the account holder's name
    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    @Column(name = "recipient_name", length = 100)
    private String fullName;

    // Contact number for the delivery agent
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter a valid 10-digit Indian mobile number starting with 6–9"
    )
    @Column(name = "phone", length = 15)
    private String phone;

    // Primary address line: flat/house number + building + street
    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    @Column(name = "street_line1", length = 200)
    private String streetLine1;

    // Secondary line: landmark or area (optional)
    @Size(max = 200, message = "Street line 2 must not exceed 200 characters")
    @Column(name = "street_line2", length = 200)
    private String streetLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Column(name = "state", length = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(
            regexp = "^[1-9][0-9]{5}$",
            message = "Enter a valid 6-digit Indian pincode"
    )
    @Column(name = "pincode", length = 10)
    private String pincode;

    /**
     * Formats the address into a single human-readable line.
     * Used in notification events and email templates.
     *
     * Example output:
     *   "Flat 4B, Sunrise Apartments, Near Metro Station, Mumbai, Maharashtra - 400058"
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(streetLine1);
        if (streetLine2 != null && !streetLine2.isBlank()) {
            sb.append(", ").append(streetLine2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" - ").append(pincode);
        return sb.toString();
    }
}
