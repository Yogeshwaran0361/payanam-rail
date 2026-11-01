package com.payanam.rail.exception;

/** Thrown for business validation errors that Bean Validation cannot express (duplicate email, wrong access code, ...). */
public class ValidationException extends RailwayException {
    public ValidationException(String message) {
        super("VALIDATION_FAILED", message);
    }
}
