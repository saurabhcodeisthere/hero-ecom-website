package com.hero.bikestore.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bike-service specific exception handlers.
 * Generic exception handling (BaseException, validation, etc.)
 * is provided by common-exception's GlobalExceptionHandler.
 *
 * Add bike-specific @ExceptionHandler methods here if needed.
 */
@RestControllerAdvice
public class BikeServiceExceptionHandler {
    // BikeAlreadyExistsException   → errorCode "CONFLICT"           → 409 (handled by common)
    // ResourceNotFoundException    → errorCode "RESOURCE_NOT_FOUND" → 404 (handled by common)
    // BadRequestException          → errorCode "BAD_REQUEST"        → 400 (handled by common)
    // InvalidPaginationException   → errorCode "BAD_REQUEST"        → 400 (handled by common)
    // InvalidSearchException       → errorCode "BAD_REQUEST"        → 400 (handled by common)
    // FileStorageException         → errorCode "FILE_STORAGE_ERROR" → 500 (handled by common)
}
