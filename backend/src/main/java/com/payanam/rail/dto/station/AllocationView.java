package com.payanam.rail.dto.station;

import java.time.LocalDateTime;

/** Result of an arrival request: ASSIGNED (platform), QUEUED (position) or REJECTED (reason). */
public record AllocationView(String trainNo, String outcome, String platformId, LocalDateTime start,
                             LocalDateTime end, int queuePosition, String message) {
}
