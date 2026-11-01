package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.model.ArrivalType;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.CancellationOutcome;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.domain.policy.PlatformAllocationPolicy;
import com.payanam.rail.exception.InvalidOperationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FACADE over one station's in-memory state: the clock, the train registry, the reservation desk,
 * the platform scheduler and the event log. Pure Java (no Spring, no database), so it can be created
 * with {@code new} in unit tests and in the Test Scenarios Lab, where every scenario gets its own
 * fresh, isolated station.
 *
 * <p>Thread safety: every public method is {@code synchronized} on the station, so concurrent HTTP
 * requests can never interleave inside a booking or an allocation.</p>
 */
public class RailwayStation {
    private final String name;
    private final SimulationClock clock;
    private final EventLog log;
    private final TrainRegistry registry;
    private final ReservationDesk desk;
    private final PlatformScheduler scheduler;

    public RailwayStation(String name, LocalDateTime clockStart, PlatformAllocationPolicy policy,
                          int clearanceBufferMinutes) {
        this.name = name;
        this.clock = new SimulationClock(clockStart);
        this.log = new EventLog();
        this.registry = new TrainRegistry();
        this.desk = new ReservationDesk(registry, log);
        this.scheduler = new PlatformScheduler(registry, clock, log, policy, clearanceBufferMinutes);
    }

    private RailwayStation(String name, SimulationClock clock, EventLog log, TrainRegistry registry,
                           ReservationDesk desk, PlatformScheduler scheduler) {
        this.name = name;
        this.clock = clock;
        this.log = log;
        this.registry = registry;
        this.desk = desk;
        this.scheduler = scheduler;
    }

    /**
     * Deep copy of the whole station, used for "impact previews": the operation is run on the copy and
     * its result shown to staff, while the live station is untouched. The copy has no listener, so it
     * sends no notifications.
     */
    public synchronized RailwayStation copy() {
        Map<Reservation, Reservation> tickets = TrainService.newCopyMap();
        Map<String, TrainService> trains = new HashMap<>();
        SimulationClock c = clock.copy();
        EventLog l = log.copy();
        TrainRegistry r = registry.copy(tickets, trains);
        ReservationDesk d = desk.copy(r, l, tickets);
        PlatformScheduler s = scheduler.copy(r, c, l, trains);
        return new RailwayStation(name, c, l, r, d, s);
    }

    /** Plugs in the outside world's listener (notifications). */
    public synchronized void setListener(StationListener listener) {
        desk.setListener(listener);
        log.setListener(event -> notifyPassengers(event, listener));
    }

    /** Platform, delay and arrival events of a train fan out to the owners of its active PNRs. */
    private void notifyPassengers(StationEvent event, StationListener listener) {
        StationEvent.Category c = event.category();
        if (event.trainNo() == null || !(c == StationEvent.Category.PLATFORM || c == StationEvent.Category.DELAY
                || c == StationEvent.Category.ARRIVAL)) {
            return;
        }
        if (!registry.contains(event.trainNo())) {
            return;
        }
        TrainService train = registry.get(event.trainNo());
        if (train.getStatus() == TrainStatus.DEPARTED) {
            return;
        }
        String kind = c == StationEvent.Category.PLATFORM ? "Platform update" : c == StationEvent.Category.DELAY ? "Delay" : "Arrival";
        for (Booking b : desk.bookingsOfTrain(event.trainNo())) {
            if (!b.isFullyCancelled()) {
                listener.onTrainUpdate(new TrainUpdateNotice(b.getOwnerUserId(), b.getPnr(), train.getTrainNo(),
                        train.getName(), kind, event.message(), event.time()));
            }
        }
    }

    // ------------------------------------------------------------ setup

    public synchronized void addPlatform(Platform platform) {
        scheduler.addPlatform(platform);
    }

    public synchronized void addTrain(TrainService train) {
        registry.add(train);
        log.record(clock.now(), StationEvent.Category.SYSTEM, false, train.getTrainNo(),
                "Train " + train.getTrainNo() + " " + train.getName() + " added to the timetable");
    }

    // ------------------------------------------------------------ reservations

    public synchronized Booking book(Long ownerUserId, String ownerName, String trainNo, List<PassengerDetails> people) {
        return desk.book(ownerUserId, ownerName, trainNo, people, clock.now());
    }

    /** Booking with an explicit timestamp (seed data booked "in the past"). */
    public synchronized Booking bookAt(Long ownerUserId, String ownerName, String trainNo,
                                       List<PassengerDetails> people, LocalDateTime when) {
        return desk.book(ownerUserId, ownerName, trainNo, people, when);
    }

    public synchronized List<CancellationOutcome> cancelBooking(String pnr, Long actorUserId, boolean staffOverride) {
        return desk.cancelBooking(pnr, actorUserId, staffOverride, clock.now());
    }

    public synchronized CancellationOutcome cancelPassenger(String pnr, int passengerNo, Long actorUserId,
                                                            boolean staffOverride) {
        return desk.cancelPassenger(pnr, passengerNo, actorUserId, staffOverride, clock.now());
    }

    // ------------------------------------------------------------ platforms & operations

    public synchronized AllocationResult requestPlatform(String trainNo, ArrivalType type) {
        return scheduler.requestPlatform(trainNo, type);
    }

    public synchronized TrainService confirmArrival(String trainNo) {
        return scheduler.confirmArrival(trainNo);
    }

    public synchronized DepartureResult confirmDeparture(String trainNo) {
        return scheduler.confirmDeparture(trainNo);
    }

    public synchronized DelayImpact reportDelay(String trainNo, int minutes, String reason) {
        return scheduler.reportDelay(trainNo, minutes, reason);
    }

    public synchronized OperationResult setMaintenance(String platformId, boolean maintenance) {
        return scheduler.setMaintenance(platformId, maintenance);
    }

    public synchronized OperationResult changePlatformLength(String platformId, int lengthMeters) {
        return scheduler.changePlatformLength(platformId, lengthMeters);
    }

    /** Timetable edit (only while the train is SCHEDULED, see TrainService#reschedule). */
    public synchronized TrainService editTrain(String trainNo, LocalDateTime arrival, LocalDateTime departure,
                                               Priority priority) {
        TrainService train = registry.get(trainNo);
        registry.reschedule(train, arrival, departure, priority);
        log.record(clock.now(), StationEvent.Category.SYSTEM, false, trainNo, "Timetable edited for " + trainNo
                + ": " + PlatformScheduler.hm(arrival) + "–" + PlatformScheduler.hm(departure) + ", " + train.getDeclaredPriority());
        return train;
    }

    public synchronized List<String> advanceClock(int minutes) {
        return scheduler.advanceClock(minutes);
    }

    public synchronized void setPolicy(PlatformAllocationPolicy policy) {
        scheduler.setPolicy(policy);
    }

    public synchronized void setClearanceBufferMinutes(int minutes) {
        scheduler.setClearanceBufferMinutes(minutes);
        log.record(clock.now(), StationEvent.Category.SYSTEM, false, null, "Clearance buffer set to " + minutes + " min");
    }

    public synchronized void setClockRunning(boolean running) {
        clock.setRunning(running);
    }

    /** Requests a platform for every SCHEDULED train in timetable order (used by the seed loader). */
    public synchronized List<AllocationResult> requestPlatformsFor(List<String> trainNos) {
        List<AllocationResult> results = new java.util.ArrayList<>();
        for (String no : trainNos) {
            if (registry.get(no).getStatus() != TrainStatus.SCHEDULED) {
                throw new InvalidOperationException("Train " + no + " already requested a platform");
            }
            results.add(scheduler.requestPlatform(no, null));
        }
        return results;
    }

    // ------------------------------------------------------------ read access (callers must not mutate)

    public String getName() {
        return name;
    }

    public SimulationClock clock() {
        return clock;
    }

    public TrainRegistry trains() {
        return registry;
    }

    public ReservationDesk reservations() {
        return desk;
    }

    public PlatformScheduler platforms() {
        return scheduler;
    }

    public EventLog events() {
        return log;
    }

    public LocalDateTime now() {
        return clock.now();
    }
}
