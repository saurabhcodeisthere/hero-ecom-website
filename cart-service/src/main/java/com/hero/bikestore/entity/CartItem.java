package com.hero.bikestore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single bike sitting in a customer's cart.
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. Unique constraint on (user_id, bike_id):
 *    A customer should have exactly ONE row per bike in their cart.
 *    Adding the same bike twice increments quantity — does not insert a duplicate row.
 *    Enforced at both DB level (unique constraint) and service level (findByUserIdAndBikeId).
 *
 * 2. Price and name are SNAPSHOTS:
 *    Captured from bike-service at add-to-cart time.
 *    If admin renames the bike or changes its price later, the cart row is unaffected.
 *    The customer sees exactly what was shown when they clicked "Add to Cart".
 *    This prevents the nasty bug where a price change mid-session silently changes the total.
 *
 * 3. No address stored here:
 *    Delivery address is only provided at checkout time (POST /cart/checkout).
 *    There is no reason to collect or store the address until the customer actually buys.
 *
 * 4. No Order relationship:
 *    Cart is completely separate from orders. At checkout, CartServiceImpl reads these
 *    rows, builds a PlaceOrderRequest, calls order-service, then deletes these rows.
 *    The cart is cleared only AFTER the order is successfully created.
 */
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "bike_id"},
        name = "uk_cart_user_bike"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Extracted from Keycloak JWT subject claim — never taken from the request body.
    // Ensures each customer's cart is isolated from every other customer's.
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "bike_id", nullable = false)
    private Long bikeId;

    // Snapshotted at add-to-cart time from bike-service response.
    // Never updated after initial insert — preserves what the customer saw.
    @Column(name = "bike_name", nullable = false, length = 200)
    private String bikeName;

    // Price per unit snapshotted at add-to-cart time.
    // Total for this line = unitPrice × quantity (computed in CartResponse builder, not stored).
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    // When this item was first added — used for ordering items in cart display
    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    // When quantity was last changed
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
