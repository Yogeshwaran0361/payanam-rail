package com.payanam.rail.domain.model;

import java.time.LocalDateTime;

/** An ordinary timetabled arrival. Rank 2 (NORMAL). */
public class ScheduledArrival extends PlatformRequest {

    public ScheduledArrival(TrainService train, LocalDateTime requestedAt) {
        super(train, requestedAt);
    }

    @Override
    public int priorityRank() {
        return Priority.NORMAL.rank();
    }

    @Override
    public ArrivalType getType() {
        return ArrivalType.SCHEDULED;
    }
}
