package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

public class InvalidPaginationException extends BaseException {
    public InvalidPaginationException(String message) {
        super(message, "BAD_REQUEST");
    }
}
