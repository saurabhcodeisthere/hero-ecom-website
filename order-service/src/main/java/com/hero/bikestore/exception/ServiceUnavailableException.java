package com.hero.bikestore.exception;

import com.hero.bikestore.common.exception.base.BaseException;

/**
 * Thrown when a downstream service (bike-service or inventory-service)
 * is unreachable after all retries are exhausted and circuit breaker opens.
 * HTTP 503 — SERVICE_UNAVAILABLE
 */
public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String serviceName) {
        super(serviceName + " is currently unavailable. Please try again in a moment.",
              "SERVICE_UNAVAILABLE");
    }
}
