package com.payanam.rail.exception;

/** Thrown when a platform operation would overlap intervals, use a too-short platform or displace a train (P1, P5). */
public class PlatformConflictException extends RailwayException {
    public PlatformConflictException(String message) {
        super("PLATFORM_CONFLICT", message);
    }
}
