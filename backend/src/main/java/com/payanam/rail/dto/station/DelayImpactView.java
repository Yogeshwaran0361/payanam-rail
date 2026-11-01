package com.payanam.rail.dto.station;

import java.time.LocalDateTime;
import java.util.List;

/** What a delay changes (rule P7). {@code preview} = true when nothing was applied yet. */
public record DelayImpactView(boolean preview, String trainNo, int minutes, String reason, String platformId,
                              LocalDateTime oldArrival, LocalDateTime oldDeparture, LocalDateTime newArrival,
                              LocalDateTime newDeparture, LocalDateTime oldOccupationEnd,
                              LocalDateTime newOccupationEnd, boolean occupationExtended, boolean requeued,
                              boolean lostPlatform, List<AllocationView> bumped, List<AllocationView> reallocated,
                              List<String> alerts, String message) {
}
