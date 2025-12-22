package com.hero.bikestore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<?> handleNotFound(InventoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(ex.getMessage()));
    }

    @ExceptionHandler(InventoryAlreadyExistsException.class)
    public ResponseEntity<?> handleConflict(InventoryAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ex.getMessage()));
    }

    private Map<String, Object> error(String message) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "message", message
        );
    }
}
