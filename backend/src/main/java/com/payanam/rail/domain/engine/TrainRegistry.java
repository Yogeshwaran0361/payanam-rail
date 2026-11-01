package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.SortedLists;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.exception.InvalidOperationException;
import com.payanam.rail.exception.TrainNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All train services of the station.
 * <ul>
 *   <li>HashMap trainNo → TrainService: O(1) lookup for every booking and operation.</li>
 *   <li>Sorted ArrayList timetable (by expected arrival, then trainNo): read in order by every screen.
 *       When a train's arrival changes (delay, edit) it is REMOVED and REINSERTED at its new position.</li>
 * </ul>
 */
public class TrainRegistry {
    /** Scope assumption: the station handles at most 6 train services. */
    public static final int MAX_TRAINS = 6;

    static final Comparator<TrainService> TIMETABLE_ORDER =
            Comparator.comparing(TrainService::getExpectedArrival).thenComparing(TrainService::getTrainNo);

    private final Map<String, TrainService> trains = new HashMap<>();
    private final List<TrainService> timetable = new ArrayList<>();

    public void add(TrainService train) {
        if (trains.containsKey(train.getTrainNo())) {
            throw new InvalidOperationException("Train " + train.getTrainNo() + " already exists");
        }
        if (trains.size() >= MAX_TRAINS) {
            throw new InvalidOperationException("The station is limited to " + MAX_TRAINS + " train services");
        }
        trains.put(train.getTrainNo(), train);
        SortedLists.insertSorted(timetable, train, TIMETABLE_ORDER);
    }

    /** O(1) lookup; unknown numbers raise TrainNotFoundException (test F3). */
    public TrainService get(String trainNo) {
        TrainService train = trainNo == null ? null : trains.get(trainNo.trim());
        if (train == null) {
            throw new TrainNotFoundException("Train " + trainNo + " is not in the Payanam Junction timetable");
        }
        return train;
    }

    public boolean contains(String trainNo) {
        return trainNo != null && trains.containsKey(trainNo.trim());
    }

    /** Applies a delay and keeps the timetable sorted: remove, change the key, reinsert. */
    public void delay(TrainService train, int minutes, String reason) {
        timetable.remove(train);
        train.applyDelay(minutes, reason);
        SortedLists.insertSorted(timetable, train, TIMETABLE_ORDER);
    }

    /** Timetable edit: remove, change the key, reinsert. */
    public void reschedule(TrainService train, LocalDateTime arrival, LocalDateTime departure, Priority priority) {
        timetable.remove(train);
        try {
            train.reschedule(arrival, departure, priority);
        } finally {
            SortedLists.insertSorted(timetable, train, TIMETABLE_ORDER);
        }
    }

    /** Trains in arrival order (read-only). */
    public List<TrainService> timetable() {
        return List.copyOf(timetable);
    }

    public int size() {
        return trains.size();
    }

    TrainRegistry copy(Map<Reservation, Reservation> ticketCopies, Map<String, TrainService> copiedTrains) {
        TrainRegistry c = new TrainRegistry();
        for (TrainService t : timetable) {
            TrainService copy = t.copy(ticketCopies);
            copiedTrains.put(copy.getTrainNo(), copy);
            c.trains.put(copy.getTrainNo(), copy);
            c.timetable.add(copy);       // same order as the original
        }
        return c;
    }
}
