package com.payanam.rail.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payanam.rail.domain.engine.PassengerDetails;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.policy.PlatformAllocationPolicy;
import com.payanam.rail.domain.user.UserAccount;
import com.payanam.rail.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds in-memory stations from the synthetic seed files: 3 platforms, 6 trains and 32 booking requests
 * from 15 passenger accounts (so Vaigai Express and Pearl City Express are full with waiting lists).
 */
@Component
public class StationFactory {
    private static final Logger log = LoggerFactory.getLogger(StationFactory.class);

    private final ObjectMapper mapper;
    private final UserAccountRepository users;

    public StationFactory(ObjectMapper mapper, UserAccountRepository users) {
        this.mapper = mapper;
        this.users = users;
    }

    /** The full demo dataset on {@code serviceDay}, clock at {@code clockStart}. */
    public RailwayStation seeded(String name, LocalDate serviceDay, LocalTime clockStart,
                                 PlatformAllocationPolicy policy, int buffer) {
        RailwayStation station = empty(name, serviceDay, clockStart, policy, buffer);
        List<SeedData.TrainSeed> trains = read("seed/trains.json", new TypeReference<>() { });
        for (SeedData.TrainSeed t : trains) {
            station.addTrain(new TrainService(t.trainNo(), t.name(), t.origin(), t.destination(),
                    serviceDay.atTime(t.arrival()), serviceDay.atTime(t.departure()), t.lengthMeters(), t.priority(),
                    t.fare()));
        }
        List<SeedData.BookingSeed> bookings = new ArrayList<>(read("seed/bookings.json", new TypeReference<>() { }));
        // Book in time order, exactly as the requests would have arrived.
        bookings.sort(Comparator.comparing((SeedData.BookingSeed b) -> -b.daysBefore()).thenComparing(SeedData.BookingSeed::time));
        int made = 0;
        for (SeedData.BookingSeed b : bookings) {
            UserAccount owner = users.findByEmailIgnoreCase(b.accountEmail()).orElse(null);
            if (owner == null) {
                log.warn("Seed booking skipped: no account {}", b.accountEmail());
                continue;
            }
            LocalDateTime when = serviceDay.minusDays(b.daysBefore()).atTime(b.time());
            List<PassengerDetails> people = b.passengers().stream()
                    .map(p -> new PassengerDetails(p.name(), p.age(), p.gender())).toList();
            Booking booking = station.bookAt(owner.getId(), owner.getFullName(), b.trainNo(), people, when);
            if (b.cancel()) {
                station.cancelBooking(booking.getPnr(), owner.getId(), false);
            }
            made++;
        }
        List<String> requestAtStart = trains.stream().filter(SeedData.TrainSeed::requestPlatformAtStart)
                .map(SeedData.TrainSeed::trainNo).toList();
        station.requestPlatformsFor(requestAtStart);
        log.info("Seed loaded: {} platforms, {} trains, {} bookings, platform requests for {}",
                station.platforms().sortedPlatforms().size(), trains.size(), made, requestAtStart);
        return station;
    }

    /** Platforms only, no trains or bookings ("Reset system"). */
    public RailwayStation empty(String name, LocalDate serviceDay, LocalTime clockStart,
                                PlatformAllocationPolicy policy, int buffer) {
        RailwayStation station = new RailwayStation(name, serviceDay.atTime(clockStart), policy, buffer);
        List<SeedData.PlatformSeed> platforms = read("seed/platforms.json", new TypeReference<>() { });
        for (SeedData.PlatformSeed p : platforms) {
            station.addPlatform(new Platform(p.id(), p.name(), p.lengthMeters()));
        }
        return station;
    }

    /** Reads one seed file from the classpath. */
    public <T> T read(String path, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read seed file " + path, e);
        }
    }
}
