package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

public class InventoryNotFoundException extends BaseException {
    public InventoryNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}
