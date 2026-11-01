package com.payanam.rail.domain.model;

import java.time.LocalDateTime;

/**
 * A HALF-OPEN interval [start, end) during which a train occupies a platform, where
 * end = departure + clearance buffer. Half-open means an interval ending at 10:00 does NOT clash
 * with one starting at 10:00, so back-to-back trains are allowed.
 */
public record OccupationInterval(String trainNo, LocalDateTime start, LocalDateTime end) {

    public OccupationInterval {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Interval end must be after start for train " + trainNo);
        }
    }

    /** Two half-open intervals [a, b) and [c, d) overlap exactly when a &lt; d AND c &lt; b. */
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    /** True if {@code time} lies inside [start, end). */
    public boolean contains(LocalDateTime time) {
        return !time.isBefore(start) && time.isBefore(end);
    }
}
