package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

public class BikeAlreadyExistsException extends BaseException {
    public BikeAlreadyExistsException(String message) {
        super(message, "CONFLICT");
    }
}
