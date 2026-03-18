package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BadRequestException;

/**
 * Thrown when inventory-service reports insufficient stock for a bike.
 * HTTP 400 — BAD_REQUEST
 */
public class InsufficientStockException extends BadRequestException {

    public InsufficientStockException(Long bikeId, int available, int requested) {
        super("Insufficient stock for bike ID " + bikeId +
              ". Available: " + available + ", Requested: " + requested);
    }
}
