package com.hero.bikestore.exception;

public class InventoryAlreadyExistsException extends RuntimeException {
    public InventoryAlreadyExistsException(String message) {
        super(message);
    }
}