package com.payanam.rail.seed;

import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.user.StaffDesignation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Shapes of the synthetic seed files in src/main/resources/seed/*.json (read with Jackson). */
public final class SeedData {

    private SeedData() {
    }

    /** platforms.json */
    public record PlatformSeed(String id, String name, int lengthMeters) {
    }

    /** trains.json; {@code requestPlatformAtStart} trains ask for a platform when the seed is loaded. */
    public record TrainSeed(String trainNo, String name, String origin, String destination, LocalTime arrival,
                            LocalTime departure, int lengthMeters, Priority priority, BigDecimal fare,
                            boolean requestPlatformAtStart) {
    }

    /** users.json */
    public record UsersSeed(List<PassengerSeed> passengers, List<StaffSeed> staff) {
    }

    /** A demo passenger account. */
    public record PassengerSeed(String fullName, String email, String mobile, LocalDate dateOfBirth, Gender gender,
                                String city, String password) {
    }

    /** A demo staff account. */
    public record StaffSeed(String fullName, String email, String mobile, String employeeId,
                            StaffDesignation designation, String password) {
    }

    /** bookings.json: one booking request, made {@code daysBefore} days before the service day at {@code time}. */
    public record BookingSeed(String accountEmail, String trainNo, int daysBefore, LocalTime time,
                              List<TravellerSeed> passengers, boolean cancel) {
    }

    /** A traveller on a seed booking. */
    public record TravellerSeed(String name, int age, Gender gender) {
    }
}
