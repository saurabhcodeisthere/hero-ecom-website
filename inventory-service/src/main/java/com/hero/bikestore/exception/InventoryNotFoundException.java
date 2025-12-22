package com.hero.bikestore.exception;


public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}
