package com.payanam.rail.service;

import com.payanam.rail.config.PayanamProperties;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.policy.AllocationPolicyType;
import com.payanam.rail.seed.StationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Owns the live in-memory {@link RailwayStation} and gives the other services synchronized access to
 * it. Also loads / resets the seed dataset, keeps the station settings across reloads and drives the
 * "play" mode of the simulation clock.
 */
@Service
public class StationService {
    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private final StationFactory factory;
    private final NotificationService notifications;
    private final PayanamProperties.Station config;
    private final Clock clock;

    private volatile RailwayStation station;
    private AllocationPolicyType policyType;
    private int clearanceBuffer;
    private long ticks;

    public StationService(StationFactory factory, NotificationService notifications, PayanamProperties properties,
                          Clock clock) {
        this.factory = factory;
        this.notifications = notifications;
        this.config = properties.station();
        this.clock = clock;
        this.policyType = config.allocationPolicy();
        this.clearanceBuffer = config.clearanceBufferMinutes();
    }

    /** Runs a read-only function while holding the station's lock. */
    public <T> T read(Function<RailwayStation, T> reader) {
        RailwayStation s = current();
        synchronized (s) {
            return reader.apply(s);
        }
    }

    /** Runs a state-changing function while holding the station's lock. */
    public <T> T write(Function<RailwayStation, T> writer) {
        RailwayStation s = current();
        synchronized (s) {
            return writer.apply(s);
        }
    }

    public void run(Consumer<RailwayStation> writer) {
        write(s -> {
            writer.accept(s);
            return null;
        });
    }

    /** "What-if": runs the function on a deep copy, so the live station is never changed. */
    public <T> T preview(Function<RailwayStation, T> writer) {
        RailwayStation copy = current().copy();
        return writer.apply(copy);
    }

    private RailwayStation current() {
        RailwayStation s = station;
        if (s == null) {
            throw new IllegalStateException("The station is still loading");
        }
        return s;
    }

    /** Loads the full seed dataset (6 trains, 32 bookings). Notifications of the old run are cleared. */
    public synchronized void loadSeed() {
        replace(factory.seeded(config.name(), serviceDay(), config.clockStart(), policyType.create(), clearanceBuffer));
    }

    /** Resets to an empty station: platforms only, no trains or bookings. */
    public synchronized void resetEmpty() {
        replace(factory.empty(config.name(), serviceDay(), config.clockStart(), policyType.create(), clearanceBuffer));
    }

    private void replace(RailwayStation fresh) {
        notifications.clear();
        fresh.setListener(notifications);
        station = fresh;
    }

    /** The service day is the day the server runs (the seed timetable is placed on it). */
    public LocalDate serviceDay() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return current().now();
    }

    // ------------------------------------------------------------ settings

    public synchronized void changeSettings(AllocationPolicyType newPolicy, int newBuffer) {
        run(s -> {
            if (newPolicy != policyType) {
                s.setPolicy(newPolicy.create());
            }
            if (newBuffer != clearanceBuffer) {
                s.setClearanceBufferMinutes(newBuffer);
            }
        });
        policyType = newPolicy;
        clearanceBuffer = newBuffer;
    }

    public AllocationPolicyType getPolicyType() {
        return policyType;
    }

    public int getClearanceBuffer() {
        return clearanceBuffer;
    }

    public PayanamProperties.Station config() {
        return config;
    }

    // ------------------------------------------------------------ clock "play" mode

    /**
     * Ticks every second; while the clock is playing, every {@code tick-seconds} it advances the
     * simulation by {@code play-step-minutes}.
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void tick() {
        RailwayStation s = station;
        if (s == null || !s.clock().isRunning()) {
            return;
        }
        ticks++;
        if (ticks % Math.max(1, config.tickSeconds()) != 0) {
            return;
        }
        try {
            List<String> events = write(st -> st.advanceClock(config.playStepMinutes()));
            if (events.size() > 1) {
                log.debug("Clock tick: {}", events);
            }
        } catch (RuntimeException e) {
            log.warn("Clock tick failed, pausing the clock: {}", e.getMessage());
            run(st -> st.setClockRunning(false));
        }
    }
}
