package com.hero.bikestore.exception.base;

public class UserBlockedException extends BaseException {
    public UserBlockedException(String message) {
        super(message, "USER_BLOCKED");
    }
}
