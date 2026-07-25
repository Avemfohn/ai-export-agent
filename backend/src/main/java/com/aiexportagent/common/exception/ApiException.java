package com.aiexportagent.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for exceptions that should be translated into a specific HTTP
 * status + JSON error body by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
