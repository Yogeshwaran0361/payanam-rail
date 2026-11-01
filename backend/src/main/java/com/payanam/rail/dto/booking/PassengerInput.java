package com.payanam.rail.dto.booking;

import com.payanam.rail.domain.model.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One traveller typed in step 2 of the booking wizard. */
public record PassengerInput(
        @NotBlank(message = "Passenger name is required") @Size(max = 60) String name,
        @Min(value = 1, message = "Age must be at least 1") @Max(value = 120, message = "Age must be at most 120") int age,
        @NotNull(message = "Gender is required") Gender gender) {
}
