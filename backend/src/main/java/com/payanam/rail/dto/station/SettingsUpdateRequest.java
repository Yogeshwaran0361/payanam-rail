package com.payanam.rail.dto.station;

import com.payanam.rail.domain.policy.AllocationPolicyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Change the allocation policy and/or the clearance buffer. */
public record SettingsUpdateRequest(@NotNull AllocationPolicyType allocationPolicy,
                                    @Min(0) @Max(60) int clearanceBufferMinutes) {
}
