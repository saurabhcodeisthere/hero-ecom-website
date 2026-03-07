package com.hero.bikestore.common.exception.model;


import java.time.LocalDateTime;

public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final String message;
    private final String errorCode;

    public ErrorResponse(String message, String errorCode) {
        this.timestamp = LocalDateTime.now();
        this.message = message;
        this.errorCode = errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
