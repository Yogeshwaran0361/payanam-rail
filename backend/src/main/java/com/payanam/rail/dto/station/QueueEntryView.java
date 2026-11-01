package com.payanam.rail.dto.station;

import java.time.LocalDateTime;

/** One row of the live PriorityQueue view, in true poll() order. */
public record QueueEntryView(int rank, String trainNo, String trainName, String type, int priorityRank,
                             LocalDateTime arrival, LocalDateTime desiredEnd, int lengthMeters,
                             LocalDateTime requestedAt, int delayMinutes, String waitReason,
                             String nextReleasePlatform, LocalDateTime nextReleaseTime) {
}
