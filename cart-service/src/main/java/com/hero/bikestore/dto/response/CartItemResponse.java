package com.hero.bikestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single item in the cart response.
 *
 * lineTotal = unitPrice × quantity
 * Computed here rather than stored in the DB — it is always derivable
 * from the two stored values and need not persist separately.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long cartItemId;
    private Long bikeId;
    private String bikeName;

    // Price per unit — snapshotted when this item was added to the cart.
    // Reflects what the customer saw when they clicked "Add to Cart".
    private BigDecimal unitPrice;

    private Integer quantity;

    // Derived: unitPrice × quantity — computed in the service layer, never stored.
    private BigDecimal lineTotal;

    private LocalDateTime addedAt;
}
