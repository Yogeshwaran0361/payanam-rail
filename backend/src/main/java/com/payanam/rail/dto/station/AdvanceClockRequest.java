package com.payanam.rail.dto.station;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Move the simulation clock forward by 1–240 minutes. */
public record AdvanceClockRequest(@Min(1) @Max(240) int minutes) {
}
