package com.payanam.rail.exception;

/** Thrown when an account is locked after too many failed sign-in attempts. */
public class AccountLockedException extends RailwayException {
    public AccountLockedException(String message) {
        super("ACCOUNT_LOCKED", message);
    }
}
