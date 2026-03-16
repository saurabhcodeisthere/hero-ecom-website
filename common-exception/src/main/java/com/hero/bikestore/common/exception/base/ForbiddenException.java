package com.hero.bikestore.common.exception.base;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }
}
