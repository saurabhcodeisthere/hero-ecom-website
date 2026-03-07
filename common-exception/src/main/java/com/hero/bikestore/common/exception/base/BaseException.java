package com.hero.bikestore.common.exception.base;

public abstract class BaseException extends RuntimeException {

    private final String errorCode;

    protected BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
