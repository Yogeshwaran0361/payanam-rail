package com.payanam.rail.exception;

/** Thrown when a user acts on data they do not own, e.g. someone else's PNR (R5), or lacks a permission. */
public class UnauthorizedActionException extends RailwayException {
    public UnauthorizedActionException(String message) {
        super("UNAUTHORIZED_ACTION", message);
    }
}
