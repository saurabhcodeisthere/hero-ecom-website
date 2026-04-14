package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BadRequestException;

/**
 * Thrown when a customer tries to add a bike that is:
 *   - Not found in bike-service (bike-service returned 404)
 *   - Marked as inactive/discontinued
 *   - Out of stock (inventory stockQuantity == 0)
 *
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler in common-exception.
 */
public class BikeNotAvailableException extends BadRequestException {

    public BikeNotAvailableException(String message) {
        super(message);
    }
}
