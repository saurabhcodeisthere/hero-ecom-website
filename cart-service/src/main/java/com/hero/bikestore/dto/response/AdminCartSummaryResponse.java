package com.hero.bikestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight cart summary for the admin list view.
 *
 * Returns one row per customer who has at least one item in their cart.
 * Used for analytics: how many customers are mid-shopping, which carts
 * have high value, which carts have been idle the longest (abandonment risk).
 *
 * Does NOT expose bikeName or other item detail — use GET /admin/carts/{userId}
 * for the full breakdown of a specific customer's cart.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCartSummaryResponse {

    // Keycloak UUID — identifies the customer
    private String userId;

    // Total number of distinct bikes in the cart
    private int itemCount;

    // Sum of (unitPrice × quantity) across all items
    private BigDecimal cartTotal;

    // When the most recent item was added — useful for spotting fresh vs abandoned carts
    private LocalDateTime lastUpdatedAt;
}
