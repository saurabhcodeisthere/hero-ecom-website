package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BadRequestException;

/**
 * Thrown when an order status transition is not allowed.
 *
 * Examples:
 *   - Trying to confirm an already CANCELLED order
 *   - Trying to cancel a DELIVERED order
 *   - Trying to ship a PENDING order (must confirm first)
 */
public class InvalidOrderStateException extends BadRequestException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
