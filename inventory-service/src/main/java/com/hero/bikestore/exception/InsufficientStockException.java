package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BadRequestException;

/**
 * Thrown when a requested quantity exceeds available stock.
 * Extends BadRequestException → errorCode: BAD_REQUEST → HTTP 400
 */
public class InsufficientStockException extends BadRequestException {

    public InsufficientStockException(Long bikeId, int available, int requested) {
        super("Insufficient stock for bike ID " + bikeId +
              ". Available: " + available + ", Requested: " + requested);
    }
}
