package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BadRequestException;

/**
 * Thrown when an event arrives with a type that has no registered handler.
 * Extends BadRequestException — maps to HTTP 400.
 */
public class UnknownEventTypeException extends BadRequestException {

    public UnknownEventTypeException(String eventType) {
        super("No handler registered for event type: " + eventType +
              ". Please ensure a handler is implemented and annotated with @Component.");
    }
}
