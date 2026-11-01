package com.payanam.rail.domain.model;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Something that happened at the station (simulation time). {@code alert} events are shown in the
 * Control Centre's alerts panel (rejections, re-queued trains, conflicts).
 *
 * @param sequence global insertion counter, the tie-breaker for events at the same minute
 */
public record StationEvent(long sequence, LocalDateTime time, Category category, boolean alert, String trainNo,
                           String message) {

    /** Event log order: simulation time, then insertion order. */
    public static final Comparator<StationEvent> ORDER =
            Comparator.comparing(StationEvent::time).thenComparingLong(StationEvent::sequence);

    /** What part of the station produced the event. */
    public enum Category { BOOKING, CANCELLATION, PROMOTION, PLATFORM, ARRIVAL, DEPARTURE, DELAY, CLOCK, SYSTEM }
}
