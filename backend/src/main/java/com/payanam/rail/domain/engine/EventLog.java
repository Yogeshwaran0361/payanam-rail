package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.model.SortedLists;
import com.payanam.rail.domain.model.StationEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Station event log kept as a SORTED ArrayList (by simulation time, then insertion order).
 * WHY sorted with binary-search insert: some events are recorded out of order (a platform release is
 * stamped with its release time, seed bookings with past times), yet the log is always read in time order.
 */
public class EventLog {
    private final List<StationEvent> events = new ArrayList<>();
    private long sequence;
    private Consumer<StationEvent> listener = e -> { };

    public StationEvent record(LocalDateTime time, StationEvent.Category category, boolean alert, String trainNo,
                               String message) {
        StationEvent event = new StationEvent(++sequence, time, category, alert, trainNo, message);
        SortedLists.insertSorted(events, event, StationEvent.ORDER);
        listener.accept(event);
        return event;
    }

    void setListener(Consumer<StationEvent> listener) {
        this.listener = listener;
    }

    EventLog copy() {
        EventLog c = new EventLog();
        c.events.addAll(events);
        c.sequence = sequence;
        return c;
    }

    /** All events, oldest first (read-only). */
    public List<StationEvent> all() {
        return List.copyOf(events);
    }

    /** Events recorded after the given sequence number (used to report what one action caused). */
    public List<StationEvent> since(long afterSequence) {
        List<StationEvent> result = new ArrayList<>();
        for (StationEvent e : events) {
            if (e.sequence() > afterSequence) {
                result.add(e);
            }
        }
        result.sort(java.util.Comparator.comparingLong(StationEvent::sequence));
        return result;
    }

    public long lastSequence() {
        return sequence;
    }
}
