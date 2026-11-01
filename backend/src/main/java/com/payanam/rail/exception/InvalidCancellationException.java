package com.payanam.rail.exception;

/** Thrown for a cancellation that breaks the rules, e.g. double cancellation (R5, test F2). */
public class InvalidCancellationException extends RailwayException {
    public InvalidCancellationException(String message) {
        super("INVALID_CANCELLATION", message);
    }
}
