package com.payanam.rail.dto.station;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Platform Management: new length. */
public record PlatformEditRequest(@Min(value = 100, message = "Length must be at least 100 m")
                                  @Max(value = 1000, message = "Length must be at most 1000 m") int lengthMeters) {
}
