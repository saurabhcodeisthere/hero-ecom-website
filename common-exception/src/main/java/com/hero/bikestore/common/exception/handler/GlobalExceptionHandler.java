package com.hero.bikestore.common.exception.handler;

import com.hero.bikestore.common.exception.base.BaseException;
import com.hero.bikestore.common.exception.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorResponse response =
                new ErrorResponse(ex.getMessage(), ex.getErrorCode());

        return ResponseEntity
                .status(mapStatus(ex))
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Validation error");

        ErrorResponse response =
                new ErrorResponse(message, "VALIDATION_ERROR");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * @PreAuthorize("hasRole('CUSTOMER')") throws AccessDeniedException when
     * the authenticated user does not have the required role.
     *
     * This is different from 401 (not authenticated) — the user IS logged in,
     * they just don't have permission. That is a 403 Forbidden.
     *
     * Without this handler, AccessDeniedException falls through to handleUnknown()
     * and returns 500 — which is wrong and confusing.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response =
                new ErrorResponse("You do not have permission to perform this action", "FORBIDDEN");
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {

        ErrorResponse response =
                new ErrorResponse("Internal server error", "INTERNAL_ERROR");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private HttpStatus mapStatus(BaseException ex) {
        return switch (ex.getErrorCode()) {
            case "RESOURCE_NOT_FOUND"    -> HttpStatus.NOT_FOUND;
            case "BAD_REQUEST"           -> HttpStatus.BAD_REQUEST;
            case "CONFLICT"              -> HttpStatus.CONFLICT;
            case "FORBIDDEN"             -> HttpStatus.FORBIDDEN;
            case "SERVICE_UNAVAILABLE"   -> HttpStatus.SERVICE_UNAVAILABLE;
            default                      -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}