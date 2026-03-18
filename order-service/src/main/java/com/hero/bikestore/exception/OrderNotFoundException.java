package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.ResourceNotFoundException;

/**
 * Thrown when an order cannot be found by ID or order number.
 * Extends ResourceNotFoundException → errorCode: RESOURCE_NOT_FOUND → HTTP 404
 */
public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id);
    }

    public OrderNotFoundException(String orderNumber) {
        super("Order not found with order number: " + orderNumber);
    }
}
