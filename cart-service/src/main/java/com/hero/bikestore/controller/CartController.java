package com.hero.bikestore.controller;

import com.hero.bikestore.dto.request.AddToCartRequest;
import com.hero.bikestore.dto.request.CheckoutRequest;
import com.hero.bikestore.dto.request.UpdateCartItemRequest;
import com.hero.bikestore.dto.response.CartResponse;
import com.hero.bikestore.dto.response.CheckoutResponse;
import com.hero.bikestore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for the shopping cart.
 *
 * All endpoints require CUSTOMER role — no public cart endpoints.
 * The userId is NEVER read from the request body or path; it always
 * comes from the Keycloak JWT (@AuthenticationPrincipal Jwt).
 *
 * Base path: /api/v1/cart
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Method  │ Path                    │ Description                    │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  POST    │ /api/v1/cart/items      │ Add a bike (or increment qty)  │
 * │  GET     │ /api/v1/cart            │ View current cart              │
 * │  PATCH   │ /api/v1/cart/items/{id} │ Change quantity of an item     │
 * │  DELETE  │ /api/v1/cart/items/{id} │ Remove a single item           │
 * │  DELETE  │ /api/v1/cart            │ Clear the entire cart          │
 * │  POST    │ /api/v1/cart/checkout   │ Convert cart → order           │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ─────────────────────────────────────────────────────────────────────────
    // ADD ITEM
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/cart/items
     *
     * Adds a bike to the cart. If the same bike is already in the cart,
     * its quantity is incremented by the requested amount instead of
     * creating a duplicate row.
     *
     * Returns 201 Created with the full updated cart.
     *
     * Possible errors:
     *   400 — bike is inactive or out of stock
     *   404 — bike does not exist in bike-service
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.addToCart(request, jwt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VIEW CART
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/cart
     *
     * Returns the customer's current cart. Empty cart = empty items list,
     * itemCount = 0, cartTotal = 0. Never returns 404.
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(cartService.getCart(jwt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE ITEM QUANTITY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/cart/items/{id}
     *
     * Sets the quantity of a specific cart item to the requested value.
     * To remove the item entirely, call DELETE /api/v1/cart/items/{id} instead.
     *
     * Returns 200 OK with the full updated cart.
     *
     * Possible errors:
     *   400 — quantity < 1 or > 10
     *   404 — item not found or belongs to a different user
     */
    @PatchMapping("/items/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(cartService.updateCartItem(id, request, jwt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REMOVE SINGLE ITEM
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/cart/items/{id}
     *
     * Removes a single item from the cart.
     * Other items in the cart are unaffected.
     *
     * Returns 200 OK with the full updated cart after removal.
     *
     * Possible errors:
     *   404 — item not found or belongs to a different user
     */
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(cartService.removeCartItem(id, jwt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR ENTIRE CART
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/cart
     *
     * Removes ALL items from the cart.
     * Use this when the customer wants to start fresh.
     *
     * Returns 204 No Content.
     */
    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal Jwt jwt) {

        cartService.clearCart(jwt);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECKOUT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/cart/checkout
     *
     * Converts all cart items into a single order in order-service.
     * The delivery address is provided in the request body — it is NOT
     * stored in the cart and is only required at the moment of purchase.
     *
     * On success:
     *   - Cart is cleared automatically
     *   - Returns 201 Created with orderId, orderNumber, status, and paymentUrl
     *   - Frontend MUST redirect the customer to paymentUrl to complete payment
     *
     * Possible errors:
     *   400 — cart is empty
     *   400 — address validation failed (missing required fields, invalid formats)
     *   503 — order-service is temporarily unavailable (cart is preserved — safe to retry)
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.checkout(request, jwt));
    }
}
