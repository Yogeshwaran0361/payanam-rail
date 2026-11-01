package com.payanam.rail.domain.model;

/**
 * A traveller named on a PNR (not a login account). Immutable: a ticket's passenger details
 * never change after booking. {@code id} is "PNR/n", unique across the station.
 */
public record Passenger(String id, String name, int age, Gender gender) {

    /** Validates the passenger details given at booking time. */
    public Passenger {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Passenger name is required");
        }
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("Passenger age must be between 1 and 120");
        }
        if (gender == null) {
            throw new IllegalArgumentException("Passenger gender is required");
        }
        name = name.trim();
    }
}
