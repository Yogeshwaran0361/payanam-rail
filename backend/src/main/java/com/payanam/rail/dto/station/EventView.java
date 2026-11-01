package com.payanam.rail.dto.station;

import java.time.LocalDateTime;

/** One station event (simulation time). Alerts are shown in the Control Centre. */
public record EventView(long sequence, LocalDateTime time, String category, boolean alert, String trainNo,
                        String message) {
}
