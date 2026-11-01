package com.payanam.rail.exception;

/** Thrown when a booking cannot be made: booking closed, departed train, or a seat-count invariant (R5, R6). */
public class SeatUnavailableException extends RailwayException {
    public SeatUnavailableException(String message) {
        super("SEAT_UNAVAILABLE", message);
    }
}
