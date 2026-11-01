package com.payanam.rail.dto.booking;

import java.time.LocalDateTime;

/** One step of a ticket timeline (Booked → WL/2 → WL/1 → CNF/S7). */
public record TimelineStepView(LocalDateTime at, String label, String note) {
}
