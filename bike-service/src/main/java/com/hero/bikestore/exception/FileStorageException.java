package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

public class FileStorageException extends BaseException {
    public FileStorageException(String message) {
        super(message, "FILE_STORAGE_ERROR");
    }

    // Uses the cause constructor we added to BaseException
    public FileStorageException(String message, Throwable cause) {
        super(message, "FILE_STORAGE_ERROR", cause);
    }
}
