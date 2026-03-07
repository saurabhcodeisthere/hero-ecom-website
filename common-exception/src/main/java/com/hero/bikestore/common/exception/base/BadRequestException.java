package com.hero.bikestore.common.exception.base;


public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
    }
}
