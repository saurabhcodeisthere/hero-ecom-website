package com.hero.bikestore.exception.base;

import com.hero.bikestore.common.exception.base.BaseException;

public class UserBlockedException extends BaseException {

    public UserBlockedException(String message) {
        super(message, "USER_BLOCKED");
    }
}
