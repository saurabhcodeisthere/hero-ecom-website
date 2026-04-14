package com.hero.bikestore.repository;

import com.hero.bikestore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Persistence layer for CartItem.
 *
 * All queries are scoped by userId — a customer can only
 * access their own cart rows. userId comes from the Keycloak
 * JWT subject claim and is never supplied by the request body.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Returns all items in a user's cart, ordered by the time they were added.
     * Used when rendering the cart page — newest items appear at the bottom.
     */
    List<CartItem> findByUserIdOrderByAddedAtAsc(String userId);

    /**
     * Finds a specific bike in a user's cart.
     *
     * Used at add-to-cart time:
     *   - If present  → increment quantity (do NOT insert a duplicate row)
     *   - If absent   → insert a new row
     *
     * The unique constraint uk_cart_user_bike enforces this at DB level too.
     */
    Optional<CartItem> findByUserIdAndBikeId(String userId, Long bikeId);

    /**
     * Deletes every item in a user's cart.
     * Called ONLY after a successful order is created at checkout.
     * Never called before — partial clearing would lose items on network failure.
     */
    void deleteAllByUserId(String userId);

    /**
     * Counts items in a user's cart.
     * Used to guard against checkout with an empty cart.
     */
    boolean existsByUserId(String userId);

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns one row per distinct userId that currently has cart items.
     * Used by the admin to see all active (non-empty) carts.
     *
     * JPQL — returns Object[] per group: [userId, itemCount, cartTotal, lastUpdatedAt]
     */
    @Query("""
            SELECT c.userId,
                   COUNT(c.id),
                   SUM(c.unitPrice * c.quantity),
                   MAX(c.updatedAt)
            FROM CartItem c
            GROUP BY c.userId
            ORDER BY MAX(c.updatedAt) DESC
            """)
    List<Object[]> findAllCartSummaries();
}
