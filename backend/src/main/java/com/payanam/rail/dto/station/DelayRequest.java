package com.payanam.rail.dto.station;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Report a delay of 1–720 minutes with an optional reason. */
public record DelayRequest(@Min(value = 1, message = "Delay must be at least 1 minute")
                           @Max(value = 720, message = "Delay must be at most 720 minutes") int minutes,
                           @Size(max = 120) String reason) {
}
