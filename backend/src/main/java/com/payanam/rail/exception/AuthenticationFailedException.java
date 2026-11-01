package com.payanam.rail.exception;

/** Thrown for a wrong user name / password combination (the message never says which one was wrong). */
public class AuthenticationFailedException extends RailwayException {
    public AuthenticationFailedException(String message) {
        super("AUTH_FAILED", message);
    }
}
