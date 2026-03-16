package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

public class InventoryAlreadyExistsException extends BaseException {
    public InventoryAlreadyExistsException(String message) {
        super(message, "CONFLICT");
    }
}