package com.payanam.rail.domain.engine;

import java.time.LocalDateTime;
import java.util.List;

/** Result of a confirmed departure (rule P8): when the platform is released and who got it. */
public record DepartureResult(String trainNo, String platformId, LocalDateTime departedAt,
                              LocalDateTime releaseAt, List<AllocationResult> allocatedFromQueue,
                              String message) {
}
