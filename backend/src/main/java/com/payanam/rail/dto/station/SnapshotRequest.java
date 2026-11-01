package com.payanam.rail.dto.station;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Label for a new snapshot. */
public record SnapshotRequest(@NotBlank @Size(max = 80) String label) {
}
