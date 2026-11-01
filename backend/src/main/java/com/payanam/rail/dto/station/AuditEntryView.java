package com.payanam.rail.dto.station;

import java.time.LocalDateTime;

/** One row of the audit log. */
public record AuditEntryView(long id, LocalDateTime timestamp, LocalDateTime simulationTime, String actor,
                             String role, String action, String details, boolean success) {
}
