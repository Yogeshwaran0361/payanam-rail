package com.payanam.rail.dto.train;

import com.payanam.rail.domain.model.Priority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Add a train service (the station allows at most 6). */
public record NewTrainRequest(
        @NotBlank @Pattern(regexp = "^\\d{5}$", message = "Train number has 5 digits") String trainNo,
        @NotBlank(message = "Name is required") @Size(max = 60) String name,
        @NotBlank(message = "Origin is required") @Size(max = 60) String origin,
        @NotBlank(message = "Destination is required") @Size(max = 60) String destination,
        @NotNull(message = "Arrival time is required") LocalTime arrival,
        @NotNull(message = "Departure time is required") LocalTime departure,
        @Min(value = 50, message = "Length must be at least 50 m") @Max(value = 1000, message = "Length must be at most 1000 m")
        int lengthMeters,
        @NotNull(message = "Priority is required") Priority priority,
        @NotNull @DecimalMin(value = "0", message = "Fare cannot be negative") BigDecimal fare) {
}
