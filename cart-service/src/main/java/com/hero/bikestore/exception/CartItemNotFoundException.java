package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.ResourceNotFoundException;

/**
 * Thrown when a cart item is not found by its ID,
 * or when the item exists but belongs to a different user (treat as 404 not 403
 * to avoid leaking information about other customers' carts).
 */
public class CartItemNotFoundException extends ResourceNotFoundException {

    public CartItemNotFoundException(Long cartItemId) {
        super("Cart item not found: " + cartItemId);
    }
}
