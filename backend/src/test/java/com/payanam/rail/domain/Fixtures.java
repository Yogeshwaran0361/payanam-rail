package com.payanam.rail.domain;

import com.payanam.rail.domain.engine.PassengerDetails;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.policy.BestFitAllocationPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Shared test data: Payanam Junction's three platforms and helpers to build trains and passengers. */
public final class Fixtures {
    public static final LocalDate DAY = LocalDate.of(2026, 1, 5);

    private Fixtures() {
    }

    public static LocalDateTime at(String hhmm) {
        return LocalDateTime.of(DAY, LocalTime.parse(hhmm));
    }

    /** Station with P1 600 m, P2 450 m, P3 300 m, clock at 06:00, best fit, 10 min buffer. */
    public static RailwayStation station() {
        RailwayStation s = new RailwayStation("Payanam Junction", at("06:00"), new BestFitAllocationPolicy(), 10);
        s.addPlatform(new Platform("P1", "Platform 1", 600));
        s.addPlatform(new Platform("P2", "Platform 2", 450));
        s.addPlatform(new Platform("P3", "Platform 3", 300));
        return s;
    }

    public static TrainService train(String no, String arrival, String departure, int length, Priority priority) {
        return new TrainService(no, "Train " + no, "Chennai Egmore", "Madurai Jn", at(arrival), at(departure), length,
                priority, new BigDecimal("450"));
    }

    public static List<PassengerDetails> people(int count) {
        List<PassengerDetails> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new PassengerDetails("Passenger " + i, 30 + i, i % 2 == 0 ? Gender.FEMALE : Gender.MALE));
        }
        return list;
    }

    public static List<PassengerDetails> person(String name) {
        return List.of(new PassengerDetails(name, 35, Gender.MALE));
    }
}
