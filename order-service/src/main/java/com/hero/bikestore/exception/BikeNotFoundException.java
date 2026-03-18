package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.ResourceNotFoundException;

/**
 * Thrown when bike-service returns 404 for the requested bike ID.
 * HTTP 404 — RESOURCE_NOT_FOUND
 */
public class BikeNotFoundException extends ResourceNotFoundException {

    public BikeNotFoundException(Long bikeId) {
        super("Bike not found with id: " + bikeId);
    }
}
