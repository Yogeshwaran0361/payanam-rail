package com.payanam.rail.domain.model;

import java.time.LocalDateTime;

/** One step of a ticket's timeline, e.g. "Booked" → "WL/2" → "WL/1" → "CNF/S7". */
public record StatusChange(LocalDateTime at, String label, String note) {
}
