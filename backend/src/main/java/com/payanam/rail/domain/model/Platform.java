package com.payanam.rail.domain.model;

import com.payanam.rail.exception.PlatformConflictException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A platform of Payanam Junction with a length and its occupation timeline.
 *
 * <p>ENCAPSULATION: the occupation list is private. The only way to add an interval is
 * {@link #occupyPlatform}, which refuses a platform under maintenance, a platform that is too short
 * and any overlapping interval. So the invariant "intervals on one platform never overlap" (P1)
 * cannot be broken from outside.</p>
 */
public class Platform {
    private static final Comparator<OccupationInterval> BY_START = Comparator.comparing(OccupationInterval::start);

    private final String id;
    private final String name;
    private int lengthMeters;
    private boolean underMaintenance;

    /*
     * Sorted ArrayList of occupation intervals, ordered by start time.
     * WHY: intervals on one platform never overlap and are kept sorted, so an overlap test only needs to
     * look at the two NEIGHBOURS of the new interval's position, found by binary search in O(log n).
     * The same order is exactly what the Gantt timeline draws, so the UI never has to sort.
     */
    private final List<OccupationInterval> occupations = new ArrayList<>();

    public Platform(String id, String name, int lengthMeters) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Platform id is required");
        }
        requirePositive(lengthMeters);
        this.id = id.trim();
        this.name = name == null || name.isBlank() ? "Platform " + id.trim() : name.trim();
        this.lengthMeters = lengthMeters;
    }

    /** Deep copy for previews (interval records are immutable, so a new list is enough). */
    public Platform copy() {
        Platform c = new Platform(id, name, lengthMeters);
        c.underMaintenance = underMaintenance;
        c.occupations.addAll(occupations);
        return c;
    }

    /** Length compatibility: the platform must be at least as long as the train. */
    public boolean canFit(int trainLengthMeters) {
        return lengthMeters >= trainLengthMeters;
    }

    /** True if the platform may receive NEW allocations (P9: maintenance excludes it). */
    public boolean isAvailableForAllocation() {
        return !underMaintenance;
    }

    /**
     * True if [start, end) does not overlap any existing occupation.
     * Binary search finds the first interval starting AFTER {@code start}; only that interval
     * (successor) and the one before it (predecessor) can possibly overlap.
     */
    public boolean isFree(LocalDateTime start, LocalDateTime end) {
        return findClash(start, end).isEmpty();
    }

    private Optional<OccupationInterval> findClash(LocalDateTime start, LocalDateTime end) {
        OccupationInterval key = new OccupationInterval("?", start, end);
        int index = SortedLists.upperBound(occupations, key, BY_START);
        if (index > 0 && occupations.get(index - 1).overlaps(start, end)) {
            return Optional.of(occupations.get(index - 1));
        }
        if (index < occupations.size() && occupations.get(index).overlaps(start, end)) {
            return Optional.of(occupations.get(index));
        }
        return Optional.empty();
    }

    /**
     * Reserves [start, end) for a train. Enforces rule P1 at the lowest level: not in maintenance,
     * long enough, and no overlap with any other interval on this platform.
     */
    public void occupyPlatform(String trainNo, LocalDateTime start, LocalDateTime end, int trainLengthMeters) {
        if (underMaintenance) {
            throw new PlatformConflictException("Platform " + id + " is under maintenance");
        }
        if (!canFit(trainLengthMeters)) {
            throw new PlatformConflictException("Platform " + id + " (" + lengthMeters + " m) is too short for train "
                    + trainNo + " (" + trainLengthMeters + " m)");
        }
        if (findOccupation(trainNo).isPresent()) {
            throw new PlatformConflictException("Train " + trainNo + " already has an interval on platform " + id);
        }
        Optional<OccupationInterval> clash = findClash(start, end);
        if (clash.isPresent()) {
            OccupationInterval c = clash.get();
            throw new PlatformConflictException("Platform " + id + " is busy: train " + c.trainNo() + " occupies "
                    + c.start().toLocalTime() + "-" + c.end().toLocalTime());
        }
        SortedLists.insertSorted(occupations, new OccupationInterval(trainNo, start, end), BY_START);
    }

    /**
     * Re-books an interval that this train ALREADY holds with new times (used for an arrived train's
     * departure trimming or overstay). Bypasses the maintenance check on purpose: P9 says trains already
     * on a platform are never displaced.
     */
    public void replaceOccupation(String trainNo, LocalDateTime start, LocalDateTime end) {
        OccupationInterval old = removeOccupation(trainNo)
                .orElseThrow(() -> new PlatformConflictException("Train " + trainNo + " has no interval on " + id));
        Optional<OccupationInterval> clash = findClash(start, end);
        if (clash.isPresent()) {
            SortedLists.insertSorted(occupations, old, BY_START);   // put it back unchanged
            throw new PlatformConflictException("Platform " + id + " is busy: train " + clash.get().trainNo());
        }
        SortedLists.insertSorted(occupations, new OccupationInterval(trainNo, start, end), BY_START);
    }

    /**
     * Re-books an interval for a train that ALREADY held a place on this platform (its old interval was
     * removed by the caller, e.g. to extend it after a delay). Only the overlap rule is checked: the length
     * and maintenance were checked when the train was first assigned, and P9 never displaces such a train.
     */
    public void reoccupy(String trainNo, LocalDateTime start, LocalDateTime end) {
        Optional<OccupationInterval> clash = findClash(start, end);
        if (clash.isPresent()) {
            throw new PlatformConflictException("Platform " + id + " is busy: train " + clash.get().trainNo());
        }
        SortedLists.insertSorted(occupations, new OccupationInterval(trainNo, start, end), BY_START);
    }

    /** Removes the train's interval (un-assignment or re-planning). */
    public Optional<OccupationInterval> removeOccupation(String trainNo) {
        Iterator<OccupationInterval> it = occupations.iterator();
        while (it.hasNext()) {
            OccupationInterval interval = it.next();
            if (interval.trainNo().equals(trainNo)) {
                it.remove();
                return Optional.of(interval);
            }
        }
        return Optional.empty();
    }

    public Optional<OccupationInterval> findOccupation(String trainNo) {
        for (OccupationInterval interval : occupations) {
            if (interval.trainNo().equals(trainNo)) {
                return Optional.of(interval);
            }
        }
        return Optional.empty();
    }

    /** All intervals that overlap [start, end), in start-time order. */
    public List<OccupationInterval> findOverlapping(LocalDateTime start, LocalDateTime end) {
        List<OccupationInterval> result = new ArrayList<>();
        for (OccupationInterval interval : occupations) {
            if (!interval.start().isBefore(end)) {
                break;                       // sorted by start: nothing later can overlap
            }
            if (interval.overlaps(start, end)) {
                result.add(interval);
            }
        }
        return result;
    }

    /** The interval covering {@code time}, if any. */
    public Optional<OccupationInterval> occupationAt(LocalDateTime time) {
        for (OccupationInterval interval : occupations) {
            if (interval.contains(time)) {
                return Optional.of(interval);
            }
        }
        return Optional.empty();
    }

    /** Earliest time at or after {@code now} that is not covered by any interval. */
    public LocalDateTime nextFreeTime(LocalDateTime now) {
        LocalDateTime t = now;
        for (OccupationInterval interval : occupations) {
            if (!interval.end().isAfter(t)) {
                continue;                    // already finished
            }
            if (interval.start().isAfter(t)) {
                break;                       // a gap starts at t
            }
            t = interval.end();              // covered: jump to its end
        }
        return t;
    }

    /** Staff edit (Platform Management). A platform can never be shortened below a train that uses it. */
    public void changeLength(int newLengthMeters, int longestTrainUsingIt) {
        requirePositive(newLengthMeters);
        if (newLengthMeters < longestTrainUsingIt) {
            throw new PlatformConflictException("Platform " + id + " cannot be shortened to " + newLengthMeters
                    + " m: a " + longestTrainUsingIt + " m train is planned on it");
        }
        lengthMeters = newLengthMeters;
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
    }

    private static void requirePositive(int lengthMeters) {
        if (lengthMeters <= 0) {
            throw new IllegalArgumentException("Platform length must be positive");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLengthMeters() {
        return lengthMeters;
    }

    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    /** Read-only view of the sorted occupations. */
    public List<OccupationInterval> getOccupations() {
        return List.copyOf(occupations);
    }

    /** Orders "P1" &lt; "P2" &lt; "P10" (shorter id first, then alphabetical). */
    public static int compareIds(String a, String b) {
        int byLength = Integer.compare(a.length(), b.length());
        return byLength != 0 ? byLength : a.compareTo(b);
    }
}
