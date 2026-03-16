package com.hero.bikestore.exception;

// Delegating to common-exception. Do not add logic here.
// Kept so existing imports in this service still compile without changes.
public class ResourceNotFoundException extends com.hero.bikestore.common.exception.base.ResourceNotFoundException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
