package com.payanam.rail.domain.engine;

import java.time.LocalDateTime;

/**
 * Outcome of a platform request: ASSIGNED to a platform, QUEUED in the PriorityQueue, or REJECTED.
 *
 * @param queuePosition 1-based position in the waiting queue when QUEUED, otherwise 0
 */
public record AllocationResult(String trainNo, Outcome outcome, String platformId, LocalDateTime start,
                               LocalDateTime end, int queuePosition, String message) {

    /** The three possible results of an arrival request. */
    public enum Outcome { ASSIGNED, QUEUED, REJECTED }
}
