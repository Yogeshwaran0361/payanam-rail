package com.payanam.rail.exception;

/** Thrown when a PNR or passenger number does not exist (test F1). */
public class ReservationNotFoundException extends RailwayException {
    public ReservationNotFoundException(String message) {
        super("PNR_NOT_FOUND", message);
    }
}
