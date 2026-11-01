package com.payanam.rail.dto.train;

import com.payanam.rail.domain.model.Priority;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Timetable edit (times on the service day) and declared priority. */
public record TrainEditRequest(@NotNull(message = "Arrival time is required") LocalTime arrival,
                               @NotNull(message = "Departure time is required") LocalTime departure,
                               @NotNull(message = "Priority is required") Priority priority) {
}
