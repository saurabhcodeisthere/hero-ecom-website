package com.hero.bikestore.common.exception.base;

public abstract class BaseException extends RuntimeException {

    private final String errorCode;

    protected BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    // Used when wrapping another exception (e.g. IOException inside FileStorageException)
    protected BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
