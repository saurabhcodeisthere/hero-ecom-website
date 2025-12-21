package com.hero.bikestore.exception;

public class BikeAlreadyExistsException extends RuntimeException {
    public BikeAlreadyExistsException(String message) {
        super(message);
    }
}
