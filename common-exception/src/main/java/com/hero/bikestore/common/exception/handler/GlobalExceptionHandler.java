package com.hero.bikestore.common.exception.handler;

import com.hero.bikestore.common.exception.base.BaseException;
import com.hero.bikestore.common.exception.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "BAD_REQUEST"        -> HttpStatus.BAD_REQUEST;
            case "CONFLICT"           -> HttpStatus.CONFLICT;
            case "FORBIDDEN"          -> HttpStatus.FORBIDDEN;
            default                   -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}