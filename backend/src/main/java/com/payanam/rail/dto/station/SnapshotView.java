package com.payanam.rail.dto.station;

import java.time.LocalDateTime;

/** A saved snapshot (the JSON holds KPIs and every train's state at that moment). */
public record SnapshotView(long id, LocalDateTime createdAt, LocalDateTime simulationTime, String label,
                           String createdBy, String summaryJson) {
}
