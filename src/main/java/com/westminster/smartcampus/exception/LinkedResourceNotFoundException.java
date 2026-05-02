package com.westminster.smartcampus.exception;

/**
 * Custom exception thrown when a client submits a payload containing
 * a reference (e.g., roomId) to a resource that does not exist.
 *
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 * 422 is more semantically accurate than 404 here because the request URI
 * is valid — the problem is a missing reference inside the JSON payload.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String field;
    private final String value;

    public LinkedResourceNotFoundException(String field, String value) {
        super("Referenced resource not found: " + field + "='" + value + "'");
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
