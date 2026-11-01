package com.payanam.rail.domain.model;

/** Kind of platform request created by station staff; each maps to a PlatformRequest subclass. */
public enum ArrivalType {
    EMERGENCY,
    CONNECTION,
    SCHEDULED;

    /** Default request type for a train, derived from its declared priority. */
    public static ArrivalType fromPriority(Priority priority) {
        return switch (priority) {
            case EMERGENCY -> EMERGENCY;
            case CONNECTION -> CONNECTION;
            case NORMAL -> SCHEDULED;
        };
    }
}
