package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

public class InvalidSearchException extends BaseException {
    public InvalidSearchException(String message) {
        super(message, "BAD_REQUEST");
    }
}
