package com.hero.bikestore.controller;

import com.hero.bikestore.dto.response.AdminCartSummaryResponse;
import com.hero.bikestore.dto.response.CartResponse;
import com.hero.bikestore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only REST API for cart management.
 *
 * Requires ADMIN role on every endpoint.
 * Admins never interact with the customer cart endpoints (/api/v1/cart) —
 * those are scoped to a JWT and self-contained.
 *
 * Base path: /api/v1/admin/carts
 *
 * ┌───────────────────────────────────────────────────────────────────────────┐
 * │  Method  │ Path                              │ Description                │
 * ├───────────────────────────────────────────────────────────────────────────┤
 * │  GET     │ /api/v1/admin/carts               │ All active carts (summary) │
 * │  GET     │ /api/v1/admin/carts/{userId}      │ One customer's full cart   │
 * │  DELETE  │ /api/v1/admin/carts/{userId}      │ Clear a customer's cart    │
 * └───────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/admin/carts")
@RequiredArgsConstructor
public class CartAdminController {

    private final CartService cartService;

    // ─────────────────────────────────────────────────────────────────────────
    // LIST ALL ACTIVE CARTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/carts
     *
     * Returns a summary row for every customer who has at least one item
     * in their cart, ordered by most recently updated first.
     *
     * Each row contains: userId, itemCount, cartTotal, lastUpdatedAt.
     *
     * Use cases:
     *   - Cart abandonment analytics (high-value carts that haven't checked out)
     *   - Monitoring how many customers are mid-shopping right now
     *   - Identifying which bikes are frequently added but never purchased
     *
     * Returns 200 OK with an empty list if no customers have active carts.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminCartSummaryResponse>> getAllCarts() {
        return ResponseEntity.ok(cartService.getAllCartSummaries());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VIEW ONE CUSTOMER'S CART
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/carts/{userId}
     *
     * Returns the full cart (all items, quantities, prices, line totals)
     * for a specific customer, identified by their Keycloak UUID.
     *
     * Use case: a customer says "my cart is wrong" — support looks it up here.
     *
     * Returns 200 OK. Returns an empty cart (not 404) if the customer
     * has no items — the customer exists, their cart is just empty.
     *
     * @param userId the Keycloak UUID of the customer (from their profile or logs)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CartResponse> getCartByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR A CUSTOMER'S CART
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/admin/carts/{userId}
     *
     * Removes ALL items from a specific customer's cart.
     *
     * Use cases:
     *   - Support clearing a corrupted or stale cart at the customer's request
     *   - Cleaning up test accounts
     *
     * This action is logged server-side with "[ADMIN] Cart cleared for userId=..."
     * Returns 204 No Content. Safe to call on an already-empty cart.
     *
     * @param userId the Keycloak UUID of the customer whose cart to clear
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearCartByUserId(@PathVariable String userId) {
        cartService.clearCartByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
