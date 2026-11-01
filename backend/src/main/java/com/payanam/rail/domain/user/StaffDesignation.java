package com.payanam.rail.domain.user;

import java.util.EnumSet;
import java.util.Set;

/**
 * Staff job titles and what each may do:
 * Station Master = everything; Platform Controller = platforms, trains, simulation;
 * Booking Clerk = reservations only.
 */
public enum StaffDesignation {
    STATION_MASTER("Station Master", EnumSet.allOf(Permission.class)),
    PLATFORM_CONTROLLER("Platform Controller", EnumSet.of(Permission.VIEW_DASHBOARD, Permission.MANAGE_PLATFORMS,
            Permission.MANAGE_TRAINS, Permission.CONTROL_SIMULATION)),
    BOOKING_CLERK("Booking Clerk", EnumSet.of(Permission.VIEW_DASHBOARD, Permission.MANAGE_RESERVATIONS));

    private final String title;
    private final Set<Permission> permissions;

    StaffDesignation(String title, Set<Permission> permissions) {
        this.title = title;
        this.permissions = permissions;
    }

    public String title() {
        return title;
    }

    /** Read-only copy of this designation's permissions. */
    public Set<Permission> permissions() {
        return EnumSet.copyOf(permissions);
    }
}
