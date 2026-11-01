package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.Fixtures;
import com.payanam.rail.domain.model.ArrivalType;
import com.payanam.rail.domain.model.OccupationInterval;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.PlatformStatus;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.domain.policy.FirstFitAllocationPolicy;
import com.payanam.rail.exception.InvalidOperationException;
import com.payanam.rail.exception.PlatformConflictException;
import com.payanam.rail.exception.TrainNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.payanam.rail.domain.Fixtures.at;
import static com.payanam.rail.domain.Fixtures.train;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformSchedulerTest {
    private RailwayStation s;

    @BeforeEach
    void setUp() {
        s = Fixtures.station();
    }

    private TrainService add(String no, String arr, String dep, int length, Priority p) {
        TrainService t = train(no, arr, dep, length, p);
        s.addTrain(t);
        return t;
    }

    private Platform platform(String id) {
        return s.platforms().getPlatform(id);
    }

    @Test
    @DisplayName("P1 / P6: a train gets the best-fit free platform for [arrival, departure + 10 min)")
    void assignsBestFit() {
        add("12635", "07:00", "07:15", 420, Priority.NORMAL);
        AllocationResult r = s.requestPlatform("12635", null);
        assertThat(r.outcome()).isEqualTo(AllocationResult.Outcome.ASSIGNED);
        assertThat(r.platformId()).isEqualTo("P2");
        assertThat(r.start()).isEqualTo(at("07:00"));
        assertThat(r.end()).isEqualTo(at("07:25"));
        assertThat(s.trains().get("12635").getStatus()).isEqualTo(TrainStatus.PLATFORM_ASSIGNED);
        assertThat(s.platforms().nextRelease()).get().extracting(rel -> rel.platformId()).isEqualTo("P2");
    }

    @Test
    @DisplayName("T2: an overlapping second train gets another platform, a third is queued")
    void overlappingArrivals() {
        add("12635", "07:00", "07:15", 420, Priority.NORMAL);
        add("16127", "07:05", "07:20", 440, Priority.NORMAL);
        add("12693", "07:10", "07:25", 430, Priority.NORMAL);
        assertThat(s.requestPlatform("12635", null).platformId()).isEqualTo("P2");
        assertThat(s.requestPlatform("16127", null).platformId()).isEqualTo("P1");
        AllocationResult third = s.requestPlatform("12693", null);
        assertThat(third.outcome()).isEqualTo(AllocationResult.Outcome.QUEUED);
        assertThat(third.queuePosition()).isEqualTo(1);
        assertThat(third.message()).contains("busy").contains("next compatible release: P2 at 07:25");
        assertThat(platform("P2").getOccupations()).hasSize(1);
    }

    @Test
    @DisplayName("P2 / T3: a train longer than every platform is REJECTED")
    void overlongRejected() {
        add("12637", "07:00", "07:15", 650, Priority.NORMAL);
        AllocationResult r = s.requestPlatform("12637", null);
        assertThat(r.outcome()).isEqualTo(AllocationResult.Outcome.REJECTED);
        assertThat(r.message()).contains("Overlong train – exceeds all platform lengths");
        assertThat(s.trains().get("12637").getStatus()).isEqualTo(TrainStatus.REJECTED);
        assertThat(s.events().all()).anyMatch(e -> e.alert() && e.message().contains("REJECTED"));
    }

    @Test
    @DisplayName("P3: a request is refused twice and unknown trains raise TrainNotFound (F3)")
    void requestValidation() {
        add("12635", "07:00", "07:15", 420, Priority.NORMAL);
        s.requestPlatform("12635", null);
        assertThatThrownBy(() -> s.requestPlatform("12635", null)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> s.requestPlatform("00000", null)).isInstanceOf(TrainNotFoundException.class);
    }

    @Test
    @DisplayName("P4 / T5: an emergency train jumps ahead of a normal train in the queue")
    void emergencyFirst() {
        add("10001", "07:00", "07:30", 590, Priority.NORMAL);
        add("10002", "07:05", "07:20", 580, Priority.NORMAL);
        add("10003", "07:10", "07:20", 580, Priority.NORMAL);
        s.requestPlatform("10001", null);                                   // P1 until 07:40
        s.requestPlatform("10002", ArrivalType.SCHEDULED);                  // queued
        s.requestPlatform("10003", ArrivalType.EMERGENCY);                  // queued but rank 0
        assertThat(s.platforms().queueOrder()).containsExactly("10003", "10002");
        assertThat(s.platforms().requestOf("10003")).get().extracting(r -> r.priorityRank()).isEqualTo(0);
    }

    @Test
    @DisplayName("P4: connection trains rank between emergency and normal trains")
    void connectionRank() {
        add("10001", "07:00", "07:30", 590, Priority.NORMAL);
        add("10002", "07:05", "07:20", 580, Priority.NORMAL);
        add("10003", "07:20", "07:30", 580, Priority.CONNECTION);
        add("10004", "07:30", "07:40", 580, Priority.EMERGENCY);
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        s.requestPlatform("10003", null);
        s.requestPlatform("10004", null);
        assertThat(s.platforms().queueOrder()).containsExactly("10004", "10003", "10002");
    }

    @Test
    @DisplayName("P5 / T7: a train standing on a platform is never displaced, even by an emergency")
    void neverDisplace() {
        add("10001", "07:00", "07:30", 590, Priority.NORMAL);
        add("10002", "07:10", "07:20", 580, Priority.EMERGENCY);
        s.requestPlatform("10001", null);
        s.advanceClock(60);                    // 07:00
        s.confirmArrival("10001");
        AllocationResult r = s.requestPlatform("10002", null);
        assertThat(r.outcome()).isEqualTo(AllocationResult.Outcome.QUEUED);
        assertThat(s.trains().get("10001").getPlatformId()).isEqualTo("P1");
        assertThat(s.trains().get("10001").getStatus()).isEqualTo(TrainStatus.ARRIVED);
        assertThat(s.platforms().statusOf(platform("P1"))).isEqualTo(PlatformStatus.OCCUPIED);
    }

    @Test
    @DisplayName("P7: a delayed WAITING train is removed and reinserted with its new rank")
    void delayReordersQueue() {
        add("10001", "07:00", "07:30", 590, Priority.NORMAL);
        add("10002", "07:05", "07:20", 580, Priority.NORMAL);
        add("10003", "07:10", "07:20", 580, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        s.requestPlatform("10003", null);
        assertThat(s.platforms().queueOrder()).containsExactly("10002", "10003");
        DelayImpact impact = s.reportDelay("10002", 15, "late crew");
        assertThat(impact.requeued()).isTrue();
        assertThat(s.platforms().queueOrder()).containsExactly("10003", "10002");
        assertThat(s.trains().timetable()).extracting(TrainService::getTrainNo).containsExactly("10001", "10003", "10002");
    }

    @Test
    @DisplayName("P7 / T4: a delay extends the occupation and a later overlapping train is re-queued")
    void delayBumpsLaterTrain() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);      // P3 07:00-07:25
        add("10002", "07:30", "07:40", 290, Priority.NORMAL);      // P3 07:30-07:50
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        assertThat(s.trains().get("10002").getPlatformId()).isEqualTo("P3");

        DelayImpact impact = s.reportDelay("10001", 20, "signal failure");
        assertThat(impact.occupationExtended()).isTrue();
        assertThat(impact.newOccupationEnd()).isEqualTo(at("07:45"));
        assertThat(impact.bumped()).singleElement().satisfies(b -> {
            assertThat(b.trainNo()).isEqualTo("10002");
            assertThat(b.outcome()).isEqualTo(AllocationResult.Outcome.ASSIGNED);
            assertThat(b.platformId()).isEqualTo("P2");              // re-allocated to the next best fit
        });
        assertThat(impact.alerts()).singleElement().asString().contains("un-assigned from P3");
        assertThat(platform("P3").findOccupation("10001")).get().extracting(OccupationInterval::end).isEqualTo(at("07:45"));
    }

    @Test
    @DisplayName("P7: a bumped train stays queued when no other platform is free")
    void bumpedStaysQueued() {
        add("10001", "07:00", "07:15", 590, Priority.NORMAL);      // P1 07:00-07:25
        add("10002", "07:30", "07:40", 590, Priority.NORMAL);      // P1 07:30-07:50
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        DelayImpact impact = s.reportDelay("10001", 20, null);
        assertThat(impact.bumped()).singleElement()
                .extracting(AllocationResult::outcome).isEqualTo(AllocationResult.Outcome.QUEUED);
        assertThat(s.trains().get("10002").getStatus()).isEqualTo(TrainStatus.WAITING_FOR_PLATFORM);
        assertThat(impact.reason()).isEqualTo("reported by staff");
        assertThat(s.events().all()).anyMatch(e -> e.category() == StationEvent.Category.DELAY && e.alert());
    }

    @Test
    @DisplayName("P7 / P5: a delayed train that would clash with an arrived train gives up its own platform")
    void delayedTrainLosesPlatform() {
        add("10001", "07:00", "07:20", 280, Priority.NORMAL);      // P3 07:00-07:30
        add("10002", "07:40", "08:00", 290, Priority.NORMAL);      // P3 07:40-08:10
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        s.advanceClock(100);                                       // 07:40, 10001 never confirmed
        s.confirmArrival("10002");
        DelayImpact impact = s.reportDelay("10001", 30, "engine fault");
        assertThat(impact.lostPlatform()).isTrue();
        assertThat(s.trains().get("10002").getPlatformId()).isEqualTo("P3");
        assertThat(s.trains().get("10002").getStatus()).isEqualTo(TrainStatus.ARRIVED);
        assertThat(s.trains().get("10001").getPlatformId()).isNotEqualTo("P3");
    }

    @Test
    @DisplayName("P7: delaying an arrived train extends its occupation; departed trains cannot be delayed")
    void delayArrived() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.advanceClock(60);
        s.confirmArrival("10001");
        DelayImpact impact = s.reportDelay("10001", 10, "loading");
        assertThat(impact.newArrival()).isEqualTo(at("07:00"));
        assertThat(impact.newDeparture()).isEqualTo(at("07:25"));
        s.confirmDeparture("10001");
        assertThatThrownBy(() -> s.reportDelay("10001", 5, "x")).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> s.reportDelay("10001", 0, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("P7: delaying a SCHEDULED train only changes its times")
    void delayScheduled() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        DelayImpact impact = s.reportDelay("10001", 10, "x");
        assertThat(impact.occupationExtended()).isFalse();
        assertThat(impact.newArrival()).isEqualTo(at("07:10"));
    }

    @Test
    @DisplayName("P8: departure releases the platform at departure + buffer and allocates the queue")
    void departureAllocatesQueue() {
        add("10001", "07:00", "07:15", 590, Priority.NORMAL);      // P1 07:00-07:25
        add("10002", "07:05", "07:20", 580, Priority.EMERGENCY);   // queued for P1
        add("10003", "07:06", "07:20", 580, Priority.NORMAL);      // queued for P1
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        s.requestPlatform("10003", null);
        s.advanceClock(60);                                        // 07:00
        s.confirmArrival("10001");
        s.advanceClock(5);                                         // 07:05 (waiting trains held at signal)
        DepartureResult r = s.confirmDeparture("10001");
        assertThat(r.releaseAt()).isEqualTo(at("07:15"));
        assertThat(r.allocatedFromQueue()).isEmpty();              // P1 is in its clearance buffer until 07:15
        assertThat(r.message()).contains("Next in the queue: 10002 (EMERGENCY)");
        List<String> events = s.advanceClock(10);                  // 07:15: release popped from the min-heap
        assertThat(events).anyMatch(e -> e.contains("Platform P1 released at 07:15"));
        assertThat(events).anyMatch(e -> e.contains("Assigned 10002 to P1"));   // emergency served first
        assertThat(s.trains().get("10002").getExpectedArrival()).isEqualTo(at("07:15"));
        assertThat(s.trains().get("10001").getStatus()).isEqualTo(TrainStatus.DEPARTED);
        assertThat(s.platforms().queueOrder()).containsExactly("10003");
        assertThat(s.platforms().earliestCompatibleRelease(s.trains().get("10003"))).get()
                .extracting(rel -> rel.trainNo()).isEqualTo("10002");   // 10001's release was popped
    }

    @Test
    @DisplayName("P8: a late departure is treated as a delay first")
    void lateDeparture() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.advanceClock(60);
        s.confirmArrival("10001");
        s.advanceClock(25);                                        // 07:25 overstays (clock extends it)
        DepartureResult r = s.confirmDeparture("10001");
        assertThat(r.departedAt()).isEqualTo(at("07:25"));
        assertThat(r.releaseAt()).isEqualTo(at("07:35"));
        assertThat(s.trains().get("10001").getDelayMinutes()).isPositive();
    }

    @Test
    @DisplayName("P9: maintenance re-queues planned trains, keeps an arrived train, and excludes the platform")
    void maintenance() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);      // P3
        add("10002", "08:00", "08:15", 290, Priority.NORMAL);      // P3 later
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        s.advanceClock(60);
        s.confirmArrival("10001");
        OperationResult r = s.setMaintenance("P3", true);
        assertThat(r.alerts()).singleElement().asString().contains("10002");
        assertThat(r.allocations()).singleElement().extracting(AllocationResult::platformId).isEqualTo("P2");
        assertThat(s.trains().get("10001").getPlatformId()).isEqualTo("P3");
        assertThat(s.platforms().statusOf(platform("P3"))).isEqualTo(PlatformStatus.MAINTENANCE);

        add("10003", "09:00", "09:15", 280, Priority.NORMAL);
        assertThat(s.requestPlatform("10003", null).platformId()).isEqualTo("P2");
        s.setMaintenance("P3", false);
        assertThat(s.platforms().statusOf(platform("P3"))).isEqualTo(PlatformStatus.OCCUPIED);
    }

    @Test
    @DisplayName("P9: when every suitable platform is in maintenance the train waits")
    void allInMaintenance() {
        add("10001", "07:00", "07:15", 590, Priority.NORMAL);
        s.setMaintenance("P1", true);
        AllocationResult r = s.requestPlatform("10001", null);
        assertThat(r.outcome()).isEqualTo(AllocationResult.Outcome.QUEUED);
        assertThat(r.message()).contains("No platform long enough is in service").contains("maintenance: P1");
        OperationResult back = s.setMaintenance("P1", false);
        assertThat(back.allocations()).singleElement().extracting(AllocationResult::platformId).isEqualTo("P1");
    }

    @Test
    @DisplayName("Arrival: requires an assigned platform and a clock at or after the arrival time")
    void arrivalValidation() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        assertThatThrownBy(() -> s.confirmArrival("10001")).hasMessageContaining("no platform yet");
        s.requestPlatform("10001", null);
        assertThatThrownBy(() -> s.confirmArrival("10001")).hasMessageContaining("not due until 07:00");
        assertThatThrownBy(() -> s.confirmDeparture("10001")).isInstanceOf(InvalidOperationException.class);
        s.advanceClock(60);
        s.confirmArrival("10001");
        assertThatThrownBy(() -> s.confirmArrival("10001")).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Arrival: a waiting train cannot arrive")
    void waitingCannotArrive() {
        add("10001", "07:00", "07:30", 590, Priority.NORMAL);
        add("10002", "07:05", "07:20", 580, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        assertThatThrownBy(() -> s.confirmArrival("10002")).hasMessageContaining("still waiting");
    }

    @Test
    @DisplayName("Clock: a waiting train past its arrival time is held at the signal (times move to now)")
    void heldAtSignal() {
        add("10001", "07:00", "07:30", 590, Priority.NORMAL);
        add("10002", "07:05", "07:20", 580, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        List<String> events = s.advanceClock(70);                  // 07:10
        assertThat(s.trains().get("10002").getExpectedArrival()).isEqualTo(at("07:10"));
        assertThat(events).anyMatch(e -> e.contains("held at the signal"));
    }

    @Test
    @DisplayName("Clock: an arrived train past its departure keeps its platform (overstay) and releases are popped")
    void overstayAndRelease() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.advanceClock(60);
        s.confirmArrival("10001");
        s.advanceClock(20);                                        // 07:20
        assertThat(s.trains().get("10001").getExpectedDeparture()).isEqualTo(at("07:20"));
        s.confirmDeparture("10001");
        List<String> events = s.advanceClock(15);
        assertThat(events).anyMatch(e -> e.contains("Platform P3 released"));
        assertThat(s.platforms().statusOf(platform("P3"))).isEqualTo(PlatformStatus.FREE);
        assertThatThrownBy(() -> s.advanceClock(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("P6: switching to first fit changes the chosen platform")
    void switchPolicy() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        s.setPolicy(new FirstFitAllocationPolicy());
        assertThat(s.requestPlatform("10001", null).platformId()).isEqualTo("P1");
        s.setClearanceBufferMinutes(5);
        assertThat(s.platforms().getClearanceBufferMinutes()).isEqualTo(5);
        assertThatThrownBy(() -> s.setClearanceBufferMinutes(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Platform management: length changes are validated and can unblock the queue")
    void changeLength() {
        add("10001", "07:00", "07:15", 420, Priority.NORMAL);
        s.requestPlatform("10001", null);                           // P2
        assertThatThrownBy(() -> s.changePlatformLength("P2", 400)).isInstanceOf(PlatformConflictException.class);
        OperationResult r = s.changePlatformLength("P3", 320);
        assertThat(r.message()).contains("300 m → 320 m");
        assertThatThrownBy(() -> s.platforms().getPlatform("P9")).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Timetable: edits keep the timetable sorted and at most 6 trains are allowed")
    void timetable() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        add("10002", "07:30", "07:45", 280, Priority.NORMAL);
        s.editTrain("10001", at("08:00"), at("08:10"), Priority.EMERGENCY);
        assertThat(s.trains().timetable()).extracting(TrainService::getTrainNo).containsExactly("10002", "10001");
        add("10003", "09:00", "09:10", 100, Priority.NORMAL);
        add("10004", "09:00", "09:10", 100, Priority.NORMAL);
        add("10005", "09:00", "09:10", 100, Priority.NORMAL);
        add("10006", "09:00", "09:10", 100, Priority.NORMAL);
        assertThatThrownBy(() -> add("10007", "09:00", "09:10", 100, Priority.NORMAL))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("6");
        assertThatThrownBy(() -> add("10006", "09:00", "09:10", 100, Priority.NORMAL))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Preview: an operation on a copy never changes the live station")
    void copyIsIsolated() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        add("10002", "07:30", "07:40", 290, Priority.NORMAL);
        s.requestPlatform("10001", null);
        s.requestPlatform("10002", null);
        s.book(1L, "A", "10001", Fixtures.people(4));
        RailwayStation preview = s.copy();
        DelayImpact impact = preview.reportDelay("10001", 20, "x");
        preview.cancelBooking(preview.reservations().bookingsOf(1L).get(0).getPnr(), 1L, false);
        assertThat(impact.bumped()).hasSize(1);
        assertThat(s.trains().get("10001").getDelayMinutes()).isZero();
        assertThat(s.trains().get("10002").getPlatformId()).isEqualTo("P3");
        assertThat(s.trains().get("10001").getConfirmedCount()).isEqualTo(4);
        assertThat(preview.trains().get("10001").getConfirmedCount()).isZero();
        assertThat(s.events().lastSequence()).isLessThan(preview.events().lastSequence());
    }

    @Test
    @DisplayName("Seed helper: requesting platforms for a list of trains")
    void requestMany() {
        add("10001", "07:00", "07:15", 280, Priority.NORMAL);
        add("10002", "07:30", "07:40", 290, Priority.NORMAL);
        assertThat(s.requestPlatformsFor(List.of("10001", "10002"))).hasSize(2);
        assertThatThrownBy(() -> s.requestPlatformsFor(List.of("10001"))).isInstanceOf(InvalidOperationException.class);
        assertThat(s.events().since(0)).isNotEmpty();
    }
}
