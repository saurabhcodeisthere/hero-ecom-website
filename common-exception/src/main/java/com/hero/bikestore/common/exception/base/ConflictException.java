package com.hero.bikestore.common.exception.base;


public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(message, "CONFLICT");
    }
}