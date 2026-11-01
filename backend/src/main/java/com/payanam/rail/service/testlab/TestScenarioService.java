package com.payanam.rail.service.testlab;

import com.payanam.rail.domain.engine.AllocationResult;
import com.payanam.rail.domain.engine.DelayImpact;
import com.payanam.rail.domain.engine.PassengerDetails;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.ArrivalType;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.domain.policy.BestFitAllocationPolicy;
import com.payanam.rail.dto.testlab.ScenarioInfo;
import com.payanam.rail.dto.testlab.ScenarioResult;
import com.payanam.rail.exception.InvalidCancellationException;
import com.payanam.rail.exception.InvalidOperationException;
import com.payanam.rail.exception.ReservationNotFoundException;
import com.payanam.rail.exception.TrainNotFoundException;
import com.payanam.rail.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Scenarios Lab: one-click demos of the rules (T1–T7) and of refused requests (F1–F4).
 * Every scenario builds its OWN fresh station (3 platforms, clock 06:00), so running the lab never
 * touches the live station that passengers and staff are using.
 */
@Service
public class TestScenarioService {
    private static final Logger log = LoggerFactory.getLogger(TestScenarioService.class);
    private static final String POSITIVE = "POSITIVE";
    private static final String FAILURE = "FAILURE";

    private final AuditService audit;
    private final HttpAccessProbe httpProbe;
    private final Clock clock;
    /** Insertion-ordered so the lab lists T1..T7 then F1..F4. */
    private final Map<String, LabScenario> scenarios = new LinkedHashMap<>();

    public TestScenarioService(AuditService audit, HttpAccessProbe httpProbe, Clock clock) {
        this.audit = audit;
        this.httpProbe = httpProbe;
        this.clock = clock;
        register(new LabScenario("T1", "Cancelled seat goes to the oldest waiting passenger", "R3", POSITIVE,
                "Fill a 10-seat train, add WL/1 and WL/2, then cancel the passenger in seat S4.",
                "WL/1 becomes CNF/S4 and WL/2 moves up to WL/1", this::t1));
        register(new LabScenario("T2", "Overlapping arrivals use another platform or wait", "P1, P3, P6", POSITIVE,
                "Three trains arrive within 15 minutes; each needs a platform at least as long as the train.",
                "1st → P2 (best fit), 2nd overlaps P2 → P1, 3rd has no free compatible platform → queued", this::t2));
        register(new LabScenario("T3", "Overlong train is rejected", "P2", POSITIVE,
                "A 650 m train asks for a platform; the longest platform is 600 m.",
                "REJECTED: overlong train, exceeds all platform lengths", this::t3));
        register(new LabScenario("T4", "Delay extends the occupation and re-queues the later train", "P7", POSITIVE,
                "Two trains planned back to back on P2; the first is delayed by 20 minutes.",
                "P2 occupation extended; the later train is un-assigned and re-queued with an alert", this::t4));
        register(new LabScenario("T5", "Emergency train jumps ahead of a normal train", "P4", POSITIVE,
                "A normal and a later emergency train both wait for P1; then P1 is released.",
                "Queue order EMERGENCY first; the emergency train gets P1 although it arrived later", this::t5));
        register(new LabScenario("T6", "Priority tie is broken deterministically", "P4", POSITIVE,
                "Two NORMAL trains with the same arrival time wait for the same platform.",
                "Order by arrival time, then by train number: 11002 before 22001", this::t6));
        register(new LabScenario("T7", "A train on a platform is never displaced", "P5", POSITIVE,
                "A normal train has arrived on P1; an emergency train that needs P1 asks for a platform.",
                "The emergency train is queued; the arrived train stays on P1", this::t7));
        register(new LabScenario("F1", "Invalid PNR", "R5", FAILURE,
                "Look up a PNR that was never issued.", "Refused with PNR_NOT_FOUND (HTTP 404)", this::f1));
        register(new LabScenario("F2", "Double cancellation", "R5", FAILURE,
                "Cancel a booking, then cancel the same booking again.",
                "Second cancellation refused with InvalidCancellationException; seat counts unchanged", this::f2));
        register(new LabScenario("F3", "Unknown train", "R5", FAILURE,
                "Book and request a platform for train 99999, which is not in the timetable.",
                "Refused with TrainNotFoundException (HTTP 404)", this::f3));
        register(new LabScenario("F4", "Passenger calling a staff API", "Security", FAILURE,
                "Sign a passenger in and call GET /api/v1/operations/dashboard over HTTP.",
                "HTTP 403 Forbidden with the JSON error body", this::f4));
    }

    private void register(LabScenario s) {
        scenarios.put(s.id(), s);
    }

    public List<ScenarioInfo> list() {
        return scenarios.values().stream()
                .map(s -> new ScenarioInfo(s.id(), s.title(), s.rule(), s.kind(), s.description())).toList();
    }

    public ScenarioResult run(String id) {
        LabScenario s = scenarios.get(id == null ? "" : id.toUpperCase());
        if (s == null) {
            throw new InvalidOperationException("Unknown test scenario " + id + "; use T1–T7 or F1–F4");
        }
        ScenarioResult result = execute(s);
        audit.record("TEST_SCENARIO_RUN", s.id() + " " + (result.passed() ? "PASS" : "FAIL") + ": " + s.title());
        return result;
    }

    public List<ScenarioResult> runAll() {
        List<ScenarioResult> results = new ArrayList<>();
        for (LabScenario s : scenarios.values()) {
            results.add(execute(s));
        }
        long passed = results.stream().filter(ScenarioResult::passed).count();
        audit.record("TEST_SCENARIO_RUN_ALL", passed + "/" + results.size() + " scenarios passed");
        return results;
    }

    private ScenarioResult execute(LabScenario s) {
        ScenarioRecorder rec = new ScenarioRecorder();
        long start = System.nanoTime();
        try {
            s.body().accept(rec);
        } catch (RuntimeException e) {            // an unexpected error is a FAIL, not a crash of the lab
            log.warn("Lab scenario {} failed unexpectedly", s.id(), e);
            rec.check("Unexpected error: " + e.getMessage(), false);
            rec.actual(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        return new ScenarioResult(s.id(), s.title(), s.rule(), s.kind(), rec.allPassed(), s.expected(), rec.actual(),
                List.copyOf(rec.steps()), ms);
    }

    // ================================================================ isolated datasets

    private LocalDateTime at(String hhmm) {
        return LocalDateTime.of(LocalDate.now(clock), LocalTime.parse(hhmm));
    }

    /** A fresh station: P1 600 m, P2 450 m, P3 300 m, clock 06:00, best fit, 10 min buffer. */
    private RailwayStation labStation() {
        RailwayStation s = new RailwayStation("Payanam Junction (lab)", at("06:00"), new BestFitAllocationPolicy(), 10);
        s.addPlatform(new Platform("P1", "Platform 1", 600));
        s.addPlatform(new Platform("P2", "Platform 2", 450));
        s.addPlatform(new Platform("P3", "Platform 3", 300));
        return s;
    }

    private TrainService train(RailwayStation s, String no, String arrival, String departure, int length,
                               Priority priority) {
        TrainService t = new TrainService(no, "Lab train " + no, "Chennai Egmore", "Madurai Jn", at(arrival),
                at(departure), length, priority, new BigDecimal("450"));
        s.addTrain(t);
        return t;
    }

    private static List<PassengerDetails> one(String name) {
        return List.of(new PassengerDetails(name, 30, Gender.FEMALE));
    }

    private static String label(Booking b) {
        return b.getReservations().get(0).getStatusLabel();
    }

    // ================================================================ positive scenarios

    private void t1(ScenarioRecorder r) {
        RailwayStation s = labStation();
        TrainService t = train(s, "12635", "07:00", "07:15", 420, Priority.NORMAL);
        List<Booking> seats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            seats.add(s.book((long) i, "Passenger " + i, "12635", one("Passenger " + i)));
        }
        r.check("Booked 10 passengers: confirmed = " + t.getConfirmedCount() + " / 10", t.getConfirmedCount() == 10);
        Booking anu = s.book(11L, "Anu", "12635", one("Anu"));
        Booking babu = s.book(12L, "Babu", "12635", one("Babu"));
        r.check("Anu is " + label(anu) + ", Babu is " + label(babu), label(anu).equals("WL/1") && label(babu).equals("WL/2"));
        Booking s4 = seats.get(3);
        r.step("Cancel PNR " + s4.getPnr() + " (" + label(s4) + ")");
        s.cancelBooking(s4.getPnr(), 4L, false);
        r.check("Anu promoted into the freed seat: " + label(anu), label(anu).equals("CNF/S4"));
        r.check("Babu moved up: " + label(babu), label(babu).equals("WL/1"));
        r.check("Seat count still within 0–10: " + t.getConfirmedCount(), t.getConfirmedCount() == 10);
        r.actual("Anu " + label(anu) + ", Babu " + label(babu));
    }

    private void t2(ScenarioRecorder r) {
        RailwayStation s = labStation();
        train(s, "10001", "07:00", "07:20", 400, Priority.NORMAL);
        train(s, "10002", "07:10", "07:30", 400, Priority.NORMAL);
        train(s, "10003", "07:15", "07:30", 420, Priority.NORMAL);
        AllocationResult a = s.requestPlatform("10001", ArrivalType.SCHEDULED);
        r.check("10001 (400 m): " + a.message(), "P2".equals(a.platformId()));
        AllocationResult b = s.requestPlatform("10002", ArrivalType.SCHEDULED);
        r.check("10002 overlaps P2 [07:00, 07:30): " + b.message(), "P1".equals(b.platformId()));
        AllocationResult c = s.requestPlatform("10003", ArrivalType.SCHEDULED);
        r.check("10003 (420 m, P3 too short): " + c.message(), c.outcome() == AllocationResult.Outcome.QUEUED);
        r.check("No platform has two overlapping intervals", s.platforms().sortedPlatforms().stream()
                .allMatch(p -> noOverlap(p)));
        r.actual("10001 → " + a.platformId() + ", 10002 → " + b.platformId() + ", 10003 → " + c.outcome());
    }

    private static boolean noOverlap(Platform p) {
        var list = p.getOccupations();
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).overlaps(list.get(i).start(), list.get(i).end())) {
                return false;
            }
        }
        return true;
    }

    private void t3(ScenarioRecorder r) {
        RailwayStation s = labStation();
        train(s, "12637", "07:45", "08:00", 650, Priority.NORMAL);
        AllocationResult a = s.requestPlatform("12637", ArrivalType.SCHEDULED);
        r.check("Outcome is REJECTED", a.outcome() == AllocationResult.Outcome.REJECTED);
        r.check("Reason: " + a.message(), a.message().toLowerCase().contains("overlong"));
        r.check("Train status is REJECTED and it is not in the queue",
                s.trains().get("12637").getStatus() == TrainStatus.REJECTED && s.platforms().queueInOrder().isEmpty());
        r.actual(a.outcome() + ": " + a.message());
    }

    private void t4(ScenarioRecorder r) {
        RailwayStation s = labStation();
        train(s, "20001", "07:00", "08:00", 500, Priority.NORMAL);
        train(s, "20002", "07:00", "07:15", 400, Priority.NORMAL);
        train(s, "20003", "07:30", "07:45", 430, Priority.NORMAL);
        s.requestPlatform("20001", ArrivalType.SCHEDULED);
        s.requestPlatform("20002", ArrivalType.SCHEDULED);
        s.requestPlatform("20003", ArrivalType.SCHEDULED);
        r.check("Plan: 20001 on P1, 20002 on P2 until 07:25, 20003 on P2 from 07:30",
                "P1".equals(s.trains().get("20001").getPlatformId()) && "P2".equals(s.trains().get("20002").getPlatformId())
                        && "P2".equals(s.trains().get("20003").getPlatformId()));
        DelayImpact d = s.reportDelay("20002", 20, "Signal failure");
        r.step(d.message());
        r.check("20002 occupation extended: ends " + d.oldOccupationEnd().toLocalTime() + " → "
                + d.newOccupationEnd().toLocalTime(), d.occupationExtended());
        r.check("20003 was bumped from P2", d.bumped().stream().anyMatch(x -> x.trainNo().equals("20003")));
        TrainService later = s.trains().get("20003");
        r.check("20003 is now " + later.getStatus() + " (P1 busy, P3 too short)", later.getStatus() == TrainStatus.WAITING_FOR_PLATFORM);
        r.check("An alert was raised for each affected train: " + d.alerts().size(), !d.alerts().isEmpty());
        r.actual("20002 extended to " + d.newOccupationEnd().toLocalTime() + "; 20003 " + later.getStatus());
    }

    private void t5(ScenarioRecorder r) {
        RailwayStation s = labStation();
        train(s, "30001", "07:00", "07:30", 550, Priority.NORMAL);
        train(s, "30002", "07:10", "07:40", 550, Priority.NORMAL);
        train(s, "06055", "07:20", "07:50", 560, Priority.EMERGENCY);
        s.requestPlatform("30001", ArrivalType.SCHEDULED);
        s.requestPlatform("30002", ArrivalType.SCHEDULED);
        s.requestPlatform("06055", ArrivalType.EMERGENCY);
        List<String> order = s.platforms().queueInOrder().stream().map(q -> q.getTrain().getTrainNo()).toList();
        r.check("Queue order " + order + " (emergency first, although it arrives later)",
                order.equals(List.of("06055", "30002")));
        s.advanceClock(60);
        s.confirmArrival("30001");
        s.advanceClock(30);
        s.confirmDeparture("30001");
        r.step("07:30 30001 departs; P1 is released at 07:40 (departure + 10 min buffer)");
        s.advanceClock(10);
        r.check("06055 (EMERGENCY) got P1: " + s.trains().get("06055").getPlatformId(),
                "P1".equals(s.trains().get("06055").getPlatformId()));
        r.check("30002 (NORMAL) is still waiting", s.trains().get("30002").getStatus() == TrainStatus.WAITING_FOR_PLATFORM);
        r.actual("Queue " + order + "; P1 → 06055");
    }

    private void t6(ScenarioRecorder r) {
        RailwayStation s = labStation();
        train(s, "40001", "07:00", "07:40", 550, Priority.NORMAL);
        train(s, "22001", "07:10", "07:30", 550, Priority.NORMAL);
        train(s, "11002", "07:10", "07:30", 550, Priority.NORMAL);
        s.requestPlatform("40001", ArrivalType.SCHEDULED);
        s.requestPlatform("22001", ArrivalType.SCHEDULED);
        s.requestPlatform("11002", ArrivalType.SCHEDULED);
        List<String> order = s.platforms().queueInOrder().stream().map(q -> q.getTrain().getTrainNo()).toList();
        r.check("Both NORMAL, both 07:10: order " + order, order.equals(List.of("11002", "22001")));
        RailwayStation again = labStation();
        train(again, "40001", "07:00", "07:40", 550, Priority.NORMAL);
        train(again, "11002", "07:10", "07:30", 550, Priority.NORMAL);
        train(again, "22001", "07:10", "07:30", 550, Priority.NORMAL);
        again.requestPlatform("40001", ArrivalType.SCHEDULED);
        again.requestPlatform("11002", ArrivalType.SCHEDULED);
        again.requestPlatform("22001", ArrivalType.SCHEDULED);
        List<String> order2 = again.platforms().queueInOrder().stream().map(q -> q.getTrain().getTrainNo()).toList();
        r.check("Requests sent in the opposite order give the same queue: " + order2, order.equals(order2));
        r.actual("Queue " + order + " both times");
    }

    private void t7(ScenarioRecorder r) {
        RailwayStation s = labStation();
        train(s, "50001", "06:30", "07:30", 580, Priority.NORMAL);
        train(s, "06055", "06:45", "07:00", 560, Priority.EMERGENCY);
        s.requestPlatform("50001", ArrivalType.SCHEDULED);
        s.advanceClock(30);
        s.confirmArrival("50001");
        r.check("06:30 50001 arrived on P1", s.trains().get("50001").getStatus() == TrainStatus.ARRIVED);
        AllocationResult e = s.requestPlatform("06055", ArrivalType.EMERGENCY);
        r.check("Emergency 06055 (560 m, only P1 fits): " + e.message(), e.outcome() == AllocationResult.Outcome.QUEUED);
        r.check("50001 is still ARRIVED on P1", "P1".equals(s.trains().get("50001").getPlatformId())
                && s.trains().get("50001").getStatus() == TrainStatus.ARRIVED);
        r.actual("06055 " + e.outcome() + " at position " + e.queuePosition() + "; 50001 stays on P1");
    }

    // ================================================================ failure scenarios

    private void f1(ScenarioRecorder r) {
        RailwayStation s = labStation();
        r.actual(r.expectError("Look up PNR 4999999999", ReservationNotFoundException.class,
                () -> s.reservations().find("4999999999")));
    }

    private void f2(ScenarioRecorder r) {
        RailwayStation s = labStation();
        TrainService t = train(s, "12635", "07:00", "07:15", 420, Priority.NORMAL);
        Booking b = s.book(1L, "Kavya", "12635", one("Kavya"));
        r.step("Booked PNR " + b.getPnr() + " (" + label(b) + ")");
        s.cancelBooking(b.getPnr(), 1L, false);
        r.check("First cancellation: " + label(b) + ", confirmed = " + t.getConfirmedCount(), t.getConfirmedCount() == 0);
        r.actual(r.expectError("Cancel PNR " + b.getPnr() + " again", InvalidCancellationException.class,
                () -> s.cancelBooking(b.getPnr(), 1L, false)));
        r.check("Seat count unchanged (never below 0): " + t.getConfirmedCount(), t.getConfirmedCount() == 0);
    }

    private void f3(ScenarioRecorder r) {
        RailwayStation s = labStation();
        String booking = r.expectError("Book on train 99999", TrainNotFoundException.class,
                () -> s.book(1L, "Kavya", "99999", one("Kavya")));
        r.expectError("Platform request for train 99999", TrainNotFoundException.class,
                () -> s.requestPlatform("99999", ArrivalType.SCHEDULED));
        r.actual(booking);
    }

    private void f4(ScenarioRecorder r) {
        HttpAccessProbe.Result res = httpProbe.passengerCallsStaffApi();
        r.step("Signed in " + res.passengerEmail() + " (role PASSENGER) with a temporary session");
        r.check("GET /api/v1/operations/dashboard → HTTP " + res.status(), res.status() == 403);
        r.check("JSON error code: " + res.code(), "FORBIDDEN".equals(res.code()));
        r.step("Temporary session revoked");
        r.actual("HTTP " + res.status() + " " + res.code());
    }
}
