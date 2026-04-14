package com.hero.bikestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full cart summary returned for GET /api/v1/cart and after every cart mutation.
 *
 * Returning the full cart after every add/update/remove means the frontend
 * never needs to make a separate GET call — one round trip keeps the UI in sync.
 *
 * cartTotal = sum of all (unitPrice × quantity) across all items.
 * Computed in the service layer, never stored.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private int itemCount;

    // Sum of lineTotal across all items — the grand total the customer will pay.
    // Computed in CartServiceImpl, never stored in the DB.
    private BigDecimal cartTotal;

    private List<CartItemResponse> items;
}
