package com.payanam.rail.domain.model;

/** Life cycle of a train at Payanam Junction. */
public enum TrainStatus {
    /** In the timetable; no platform requested yet. */
    SCHEDULED,
    /** Requested a platform, but every suitable platform is busy: waiting in the PriorityQueue (P3). */
    WAITING_FOR_PLATFORM,
    /** A platform interval is reserved; the train is not yet at the platform. */
    PLATFORM_ASSIGNED,
    /** Arrival confirmed: physically on the platform, can never be displaced (P5). Booking is closed (R6). */
    ARRIVED,
    /** Departure confirmed: the platform is released at departure + clearance buffer (P8). */
    DEPARTED,
    /** No platform at the station is long enough (P2). */
    REJECTED;

    /** R6: booking closes once the train has arrived or departed (a rejected train never calls here). */
    public boolean bookingOpen() {
        return this != ARRIVED && this != DEPARTED && this != REJECTED;
    }
}
