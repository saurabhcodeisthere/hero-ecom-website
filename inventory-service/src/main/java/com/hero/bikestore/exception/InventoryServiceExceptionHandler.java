package com.hero.bikestore.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Inventory-service specific exception handlers.
 * Generic exception handling (BaseException, validation, etc.)
 * is provided by common-exception's GlobalExceptionHandler.
 *
 * Add inventory-specific @ExceptionHandler methods here if needed.
 */
@RestControllerAdvice
public class InventoryServiceExceptionHandler {
    // InventoryNotFoundException       → errorCode "RESOURCE_NOT_FOUND" → 404 (handled by common)
    // InventoryAlreadyExistsException  → errorCode "CONFLICT"           → 409 (handled by common)
}
