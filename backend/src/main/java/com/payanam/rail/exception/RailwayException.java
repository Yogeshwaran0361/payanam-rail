package com.payanam.rail.exception;

/**
 * Base class of every business-rule violation in Payanam Rail.
 * Each subclass carries a stable machine-readable {@code code} (used by the frontend and in the
 * JSON error body) and a human-readable message. The HTTP status is chosen in
 * {@link GlobalExceptionHandler}, so the domain layer stays free of web concerns.
 */
public abstract class RailwayException extends RuntimeException {
    private final String code;

    protected RailwayException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
