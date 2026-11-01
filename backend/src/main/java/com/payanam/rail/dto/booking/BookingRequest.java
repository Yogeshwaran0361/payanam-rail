package com.payanam.rail.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Book 1–4 passengers on one train (rule R2). */
public record BookingRequest(
        @NotBlank(message = "Choose a train") String trainNo,
        @NotNull @Size(min = 1, max = 4, message = "A booking has 1 to 4 passengers") List<@Valid PassengerInput> passengers) {
}
