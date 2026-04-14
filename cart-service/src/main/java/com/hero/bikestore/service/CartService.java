package com.hero.bikestore.service;

import com.hero.bikestore.dto.request.AddToCartRequest;
import com.hero.bikestore.dto.request.CheckoutRequest;
import com.hero.bikestore.dto.request.UpdateCartItemRequest;
import com.hero.bikestore.dto.response.AdminCartSummaryResponse;
import com.hero.bikestore.dto.response.CartResponse;
import com.hero.bikestore.dto.response.CheckoutResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Cart operations available to the customer.
 *
 * All methods receive the Jwt so the service can extract the userId
 * from the token's "sub" claim — never from the request body.
 */
public interface CartService {

    /**
     * Adds a bike to the cart, or increments its quantity if already present.
     * Performs a soft stock check before adding.
     *
     * @return the full updated cart
     */
    CartResponse addToCart(AddToCartRequest request, Jwt jwt);

    /**
     * Returns the customer's current cart (empty if no items).
     */
    CartResponse getCart(Jwt jwt);

    /**
     * Changes the quantity of an existing cart item.
     *
     * @param cartItemId the DB id of the CartItem row
     * @return the full updated cart
     */
    CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request, Jwt jwt);

    /**
     * Removes a single item from the cart.
     * The cart remains in place — other items are unaffected.
     *
     * @return the full updated cart after removal
     */
    CartResponse removeCartItem(Long cartItemId, Jwt jwt);

    /**
     * Removes all items from the cart.
     * Called explicitly if the customer wants a fresh start before checkout.
     */
    void clearCart(Jwt jwt);

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN OPERATIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a summary (userId, itemCount, cartTotal, lastUpdatedAt)
     * for every customer who currently has items in their cart.
     *
     * Used for analytics (cart abandonment, popular bikes) and support
     * (seeing which customers are stuck mid-checkout).
     */
    List<AdminCartSummaryResponse> getAllCartSummaries();

    /**
     * Returns the full cart contents for a specific customer.
     * Used by support staff when a customer reports an issue with their cart.
     *
     * @param userId the Keycloak UUID of the customer
     */
    CartResponse getCartByUserId(String userId);

    /**
     * Clears a specific customer's cart entirely.
     * Admin-only operation — used by support to fix stuck carts.
     *
     * @param userId the Keycloak UUID of the customer whose cart to clear
     */
    void clearCartByUserId(String userId);

    /**
     * Places an order for all items currently in the cart.
     *
     * Flow:
     *   1. Load all CartItems for this user.
     *   2. Reject if cart is empty.
     *   3. Build CartPlaceOrderRequest (cart items + delivery address).
     *   4. Call order-service with the customer's JWT forwarded.
     *   5. If order creation succeeds → clear the cart.
     *   6. Return CheckoutResponse with orderId, orderNumber, status, paymentUrl.
     *
     * The cart is NEVER cleared before step 5 — failure safety guarantee.
     *
     * @return checkout result including the paymentUrl to redirect the customer to
     */
    CheckoutResponse checkout(CheckoutRequest request, Jwt jwt);
}
