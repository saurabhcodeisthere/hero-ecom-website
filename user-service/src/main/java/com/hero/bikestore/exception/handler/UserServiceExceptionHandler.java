package com.hero.bikestore.exception.handler;

import com.hero.bikestore.common.exception.model.ErrorResponse;
import com.hero.bikestore.exception.base.UserBlockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserServiceExceptionHandler {

    // All common handlers (BaseException, Validation, Unknown) are provided
    // by com.hero.bikestore.common.exception.handler.GlobalExceptionHandler
    // which is auto-loaded from the common-exception library on the classpath.

    // Only user-service specific exception is handled here:
    @ExceptionHandler(UserBlockedException.class)
    public ResponseEntity<ErrorResponse> handleBlocked(UserBlockedException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getErrorCode());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
}
