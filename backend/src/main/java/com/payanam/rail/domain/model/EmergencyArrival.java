package com.payanam.rail.domain.model;

import java.time.LocalDateTime;

/** A declared emergency train (medical, relief, breakdown). Always served first: rank 0. */
public class EmergencyArrival extends PlatformRequest {

    public EmergencyArrival(TrainService train, LocalDateTime requestedAt) {
        super(train, requestedAt);
    }

    @Override
    public int priorityRank() {
        return Priority.EMERGENCY.rank();
    }

    @Override
    public ArrivalType getType() {
        return ArrivalType.EMERGENCY;
    }
}
