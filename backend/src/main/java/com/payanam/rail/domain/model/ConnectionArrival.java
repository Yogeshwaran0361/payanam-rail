package com.payanam.rail.domain.model;

import java.time.LocalDateTime;

/** A train carrying passengers for a connecting service. Rank 1: after emergencies, before normal trains. */
public class ConnectionArrival extends PlatformRequest {

    public ConnectionArrival(TrainService train, LocalDateTime requestedAt) {
        super(train, requestedAt);
    }

    @Override
    public int priorityRank() {
        return Priority.CONNECTION.rank();
    }

    @Override
    public ArrivalType getType() {
        return ArrivalType.CONNECTION;
    }
}
