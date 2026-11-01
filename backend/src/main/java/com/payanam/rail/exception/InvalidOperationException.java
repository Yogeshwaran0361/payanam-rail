package com.payanam.rail.exception;

/** Thrown when an operation is not allowed in the current state (wrong train status, clock not reached, ...). */
public class InvalidOperationException extends RailwayException {
    public InvalidOperationException(String message) {
        super("INVALID_OPERATION", message);
    }
}
