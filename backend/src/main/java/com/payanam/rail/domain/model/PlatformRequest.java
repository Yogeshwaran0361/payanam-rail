package com.payanam.rail.domain.model;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * ABSTRACT request of a train for a platform.
 *
 * <p>INHERITANCE + POLYMORPHISM: each subclass overrides {@link #priorityRank()}. The waiting
 * PriorityQueue only calls priorityRank() and never asks "which subclass are you?", so a new kind
 * of request means a new subclass, not an edit of the queue logic.</p>
 */
public abstract class PlatformRequest {

    /**
     * Queue order (rule P4): priorityRank, then expected arrival time, then trainNo.
     * trainNo is unique, so two different requests never compare as equal: the order is deterministic (T6).
     */
    public static final Comparator<PlatformRequest> QUEUE_ORDER =
            Comparator.comparingInt(PlatformRequest::priorityRank)
                    .thenComparing(PlatformRequest::getArrivalTime)
                    .thenComparing(PlatformRequest::getTrainNo);

    private final TrainService train;
    private final LocalDateTime requestedAt;

    protected PlatformRequest(TrainService train, LocalDateTime requestedAt) {
        this.train = train;
        this.requestedAt = requestedAt;
    }

    /** 0 = served first. Overridden by every subclass. */
    public abstract int priorityRank();

    /** The request type shown on the dashboard. */
    public abstract ArrivalType getType();

    /** Factory: builds the right subclass for the type chosen by staff. */
    public static PlatformRequest create(ArrivalType type, TrainService train, LocalDateTime requestedAt) {
        return switch (type) {
            case EMERGENCY -> new EmergencyArrival(train, requestedAt);
            case CONNECTION -> new ConnectionArrival(train, requestedAt);
            case SCHEDULED -> new ScheduledArrival(train, requestedAt);
        };
    }

    /** Same request type and time, attached to another (copied) train object. */
    public PlatformRequest copyFor(TrainService copiedTrain) {
        return create(getType(), copiedTrain, requestedAt);
    }

    public TrainService getTrain() {
        return train;
    }

    public String getTrainNo() {
        return train.getTrainNo();
    }

    /** Read live from the train, so a delay changes the ranking (hence remove + reinsert, rule P7). */
    public LocalDateTime getArrivalTime() {
        return train.getExpectedArrival();
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
}
