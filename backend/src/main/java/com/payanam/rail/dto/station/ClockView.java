package com.payanam.rail.dto.station;

import java.time.LocalDateTime;
import java.util.List;

/** Simulation clock state, plus what the last clock step caused. */
public record ClockView(LocalDateTime now, LocalDateTime start, long elapsedMinutes, boolean running,
                        int tickSeconds, int playStepMinutes, List<String> events) {
}
