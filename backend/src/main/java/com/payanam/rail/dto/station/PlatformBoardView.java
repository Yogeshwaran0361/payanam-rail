package com.payanam.rail.dto.station;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the Platform Board needs: the platforms with their timelines, the waiting trains (drawn as
 * "wanted" bars that clash = conflicts), the "now" line and the time window of the Gantt chart.
 */
public record PlatformBoardView(LocalDateTime now, LocalDateTime windowStart, LocalDateTime windowEnd,
                                int clearanceBufferMinutes, String policy, List<PlatformView> platforms,
                                List<QueueEntryView> waiting, String nextRelease) {
}
