package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.Fixtures;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.CancellationOutcome;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.ReservationStatus;
import com.payanam.rail.exception.InvalidCancellationException;
import com.payanam.rail.exception.ReservationNotFoundException;
import com.payanam.rail.exception.SeatUnavailableException;
import com.payanam.rail.exception.TrainNotFoundException;
import com.payanam.rail.exception.UnauthorizedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.payanam.rail.domain.Fixtures.people;
import static com.payanam.rail.domain.Fixtures.person;
import static com.payanam.rail.domain.Fixtures.train;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationDeskTest {
    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;
    private RailwayStation station;
    private final List<PromotionNotice> notices = new ArrayList<>();

    @BeforeEach
    void setUp() {
        station = Fixtures.station();
        station.addTrain(train("12635", "07:00", "07:15", 420, Priority.NORMAL));
        station.setListener(notices::add);
    }

    @Test
    @DisplayName("R2: one PNR holds up to 4 passengers, each with their own seat")
    void multiPassenger() {
        Booking b = station.book(ALICE, "Alice", "12635", people(4));
        assertThat(b.getReservations()).extracting(r -> r.getStatusLabel())
                .containsExactly("CNF/S1", "CNF/S2", "CNF/S3", "CNF/S4");
        assertThat(b.getPnr()).hasSize(10).startsWith("4");
        assertThat(b.totalFare()).isEqualByComparingTo("1800");
        assertThat(station.reservations().findPassenger(b.getPnr() + "/2").name()).isEqualTo("Passenger 2");
        station.cancelPassenger(b.getPnr(), 2, ALICE, false);
        assertThat(b.totalFare()).as("cancelled tickets no longer count").isEqualByComparingTo("1350");
    }

    @Test
    @DisplayName("R2: a PNR spanning the last seats is split CNF and WL per passenger")
    void mixedStatus() {
        station.book(BOB, "Bob", "12635", people(4));
        station.book(BOB, "Bob", "12635", people(4));
        Booking b = station.book(ALICE, "Alice", "12635", people(4));
        assertThat(b.getReservations()).extracting(r -> r.getStatusLabel())
                .containsExactly("CNF/S9", "CNF/S10", "WL/1", "WL/2");
    }

    @Test
    @DisplayName("R2: 0 or 5 passengers are rejected")
    void passengerLimits() {
        assertThatThrownBy(() -> station.book(ALICE, "Alice", "12635", people(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> station.book(ALICE, "Alice", "12635", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("R3: cancelling a PNR promotes the oldest waiting passenger and notifies its owner")
    void promotionNotifies() {
        Booking first = station.book(BOB, "Bob", "12635", people(4));
        station.book(BOB, "Bob", "12635", people(4));
        station.book(BOB, "Bob", "12635", people(2));
        Booking waiting = station.book(ALICE, "Alice", "12635", person("Asha"));
        assertThat(waiting.getReservations().get(0).getStatusLabel()).isEqualTo("WL/1");

        CancellationOutcome outcome = station.cancelPassenger(first.getPnr(), 3, BOB, false);
        assertThat(outcome.promoted().getPnr()).isEqualTo(waiting.getPnr());
        assertThat(waiting.getReservations().get(0).getStatusLabel()).isEqualTo("CNF/S3");
        assertThat(notices).singleElement().satisfies(n -> {
            assertThat(n.ownerUserId()).isEqualTo(ALICE);
            assertThat(n.fromLabel()).isEqualTo("WL/1");
            assertThat(n.toLabel()).isEqualTo("CNF/S3");
        });
    }

    @Test
    @DisplayName("R4: partial cancellation cancels only one passenger of a PNR")
    void partialCancel() {
        Booking b = station.book(ALICE, "Alice", "12635", people(3));
        station.cancelPassenger(b.getPnr(), 2, ALICE, false);
        assertThat(b.getReservations()).extracting(r -> r.getStatus())
                .containsExactly(ReservationStatus.CNF, ReservationStatus.CAN, ReservationStatus.CNF);
        assertThat(b.activeReservations()).hasSize(2);
        assertThat(b.isFullyCancelled()).isFalse();
        List<CancellationOutcome> rest = station.cancelBooking(b.getPnr(), ALICE, false);
        assertThat(rest).hasSize(2);
        assertThat(b.isFullyCancelled()).isTrue();
    }

    @Test
    @DisplayName("R5 / F2: cancelling a fully cancelled PNR again is rejected")
    void doubleCancelPnr() {
        Booking b = station.book(ALICE, "Alice", "12635", people(1));
        station.cancelBooking(b.getPnr(), ALICE, false);
        assertThatThrownBy(() -> station.cancelBooking(b.getPnr(), ALICE, false))
                .isInstanceOf(InvalidCancellationException.class);
        assertThatThrownBy(() -> station.cancelPassenger(b.getPnr(), 1, ALICE, false))
                .isInstanceOf(InvalidCancellationException.class);
    }

    @Test
    @DisplayName("R5 / F1: an unknown PNR or passenger number is rejected")
    void unknownPnr() {
        assertThatThrownBy(() -> station.reservations().find("0000000000"))
                .isInstanceOf(ReservationNotFoundException.class);
        Booking b = station.book(ALICE, "Alice", "12635", people(1));
        assertThatThrownBy(() -> station.cancelPassenger(b.getPnr(), 2, ALICE, false))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThatThrownBy(() -> station.reservations().findPassenger("nope"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("R5: someone else's PNR cannot be cancelled or opened, but staff may cancel it")
    void ownership() {
        Booking b = station.book(ALICE, "Alice", "12635", people(1));
        assertThatThrownBy(() -> station.cancelBooking(b.getPnr(), BOB, false))
                .isInstanceOf(UnauthorizedActionException.class);
        assertThatThrownBy(() -> station.reservations().findOwned(b.getPnr(), BOB))
                .isInstanceOf(UnauthorizedActionException.class);
        assertThat(station.reservations().findOwned(b.getPnr(), ALICE)).isSameAs(b);
        station.cancelBooking(b.getPnr(), 99L, true);
        assertThat(b.isFullyCancelled()).isTrue();
    }

    @Test
    @DisplayName("R5 / F3: booking an unknown train is rejected")
    void unknownTrain() {
        assertThatThrownBy(() -> station.book(ALICE, "Alice", "99999", people(1)))
                .isInstanceOf(TrainNotFoundException.class);
    }

    @Test
    @DisplayName("R6: booking a departed train is rejected")
    void departedTrain() {
        station.requestPlatform("12635", null);
        station.advanceClock(60);
        station.confirmArrival("12635");
        assertThatThrownBy(() -> station.book(ALICE, "Alice", "12635", people(1)))
                .isInstanceOf(SeatUnavailableException.class);
        station.confirmDeparture("12635");
        assertThatThrownBy(() -> station.book(ALICE, "Alice", "12635", people(1)))
                .isInstanceOf(SeatUnavailableException.class).hasMessageContaining("DEPARTED");
    }

    @Test
    @DisplayName("DS: My Bookings uses the owner index and history stays sorted by booking time")
    void ownerIndexAndHistory() {
        Booking late = station.bookAt(ALICE, "Alice", "12635", people(1), Fixtures.at("05:00"));
        Booking early = station.bookAt(BOB, "Bob", "12635", people(1), Fixtures.at("04:00"));
        Booking mid = station.bookAt(ALICE, "Alice", "12635", people(1), Fixtures.at("04:30"));
        assertThat(station.reservations().history()).containsExactly(early, mid, late);
        assertThat(station.reservations().bookingsOf(ALICE)).containsExactly(late, mid);
        assertThat(station.reservations().bookingsOf(42L)).isEmpty();
        assertThat(station.reservations().bookingsOfTrain("12635")).hasSize(3);
    }

    @Test
    @DisplayName("PNR numbers are unique 10-digit numbers")
    void uniquePnrs() {
        java.util.Set<String> pnrs = new java.util.HashSet<>();
        for (int i = 0; i < 40; i++) {
            pnrs.add(station.book(ALICE, "Alice", "12635", people(1)).getPnr());
        }
        assertThat(pnrs).hasSize(40).allMatch(p -> p.matches("\\d{10}"));
    }
}
