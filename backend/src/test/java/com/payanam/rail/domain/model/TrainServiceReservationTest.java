package com.payanam.rail.domain.model;

import com.payanam.rail.exception.InvalidCancellationException;
import com.payanam.rail.exception.InvalidOperationException;
import com.payanam.rail.exception.SeatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.payanam.rail.domain.Fixtures.at;
import static com.payanam.rail.domain.Fixtures.train;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainServiceReservationTest {
    private TrainService train;
    private int counter;

    @BeforeEach
    void setUp() {
        train = train("12635", "07:00", "07:15", 420, Priority.NORMAL);
    }

    private Reservation ticket() {
        counter++;
        return new Reservation("PNR" + counter, 1, new Passenger("PNR" + counter + "/1", "P" + counter, 30,
                Gender.MALE), "12635", at("06:00"));
    }

    private List<Reservation> fill(int n) {
        List<Reservation> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Reservation r = ticket();
            train.bookSeat(r, at("06:00"));
            list.add(r);
        }
        return list;
    }

    @Test
    @DisplayName("R1: the first 10 tickets are CNF with seats 1-10")
    void firstTenConfirmed() {
        List<Reservation> tickets = fill(10);
        for (int i = 0; i < 10; i++) {
            assertThat(tickets.get(i).getStatus()).isEqualTo(ReservationStatus.CNF);
            assertThat(tickets.get(i).getStatusLabel()).isEqualTo("CNF/S" + (i + 1));
        }
        assertThat(train.getConfirmedCount()).isEqualTo(10);
        assertThat(train.getSeatsLeft()).isZero();
    }

    @Test
    @DisplayName("R1: the 11th and 12th tickets get WL/1 and WL/2 in FIFO order")
    void overflowWaitlisted() {
        List<Reservation> tickets = fill(12);
        assertThat(tickets.get(10).getStatusLabel()).isEqualTo("WL/1");
        assertThat(tickets.get(11).getStatusLabel()).isEqualTo("WL/2");
        assertThat(train.getWaitingListSnapshot()).containsExactly(tickets.get(10), tickets.get(11));
        assertThat(train.getConfirmedCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("R3 / T1: cancelling CNF/S4 promotes the OLDEST waiting passenger into seat 4")
    void cancelPromotesOldest() {
        List<Reservation> tickets = fill(12);
        CancellationOutcome outcome = train.cancelBooking(tickets.get(3), at("06:30"));
        assertThat(outcome.freedSeat()).isEqualTo(4);
        assertThat(outcome.promoted()).isSameAs(tickets.get(10));
        assertThat(outcome.previousLabel()).isEqualTo("WL/1");
        assertThat(tickets.get(10).getStatusLabel()).isEqualTo("CNF/S4");
        assertThat(tickets.get(10).isPromoted()).isTrue();
        assertThat(tickets.get(11).getStatusLabel()).isEqualTo("WL/1");
        assertThat(tickets.get(3).getStatus()).isEqualTo(ReservationStatus.CAN);
        assertThat(tickets.get(3).getLastSeatNo()).isEqualTo(4);
        assertThat(train.getConfirmedCount()).isEqualTo(10);
        assertThat(tickets.get(11).getTimeline()).extracting(StatusChange::label).containsExactly("Booked", "WL/2", "WL/1");
    }

    @Test
    @DisplayName("R3: cancelling a waitlisted ticket only removes it from the queue")
    void cancelWaitlisted() {
        List<Reservation> tickets = fill(13);
        CancellationOutcome outcome = train.cancelBooking(tickets.get(11), at("06:30"));
        assertThat(outcome.freedSeat()).isNull();
        assertThat(outcome.hasPromotion()).isFalse();
        assertThat(tickets.get(12).getStatusLabel()).isEqualTo("WL/2");
        assertThat(train.getWaitingCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("R3: cancelling with an empty waiting list just frees the seat")
    void cancelWithoutWaiting() {
        List<Reservation> tickets = fill(3);
        CancellationOutcome outcome = train.cancelBooking(tickets.get(0), at("06:30"));
        assertThat(outcome.hasPromotion()).isFalse();
        assertThat(train.getConfirmedCount()).isEqualTo(2);
        Reservation next = ticket();
        train.bookSeat(next, at("06:40"));
        assertThat(next.getSeatNo()).isEqualTo(1);              // lowest free seat is reused
    }

    @Test
    @DisplayName("R5 / F2: double cancellation is rejected")
    void doubleCancel() {
        Reservation r = fill(1).get(0);
        train.cancelBooking(r, at("06:30"));
        assertThatThrownBy(() -> train.cancelBooking(r, at("06:31")))
                .isInstanceOf(InvalidCancellationException.class).hasMessageContaining("already cancelled");
        assertThat(train.getConfirmedCount()).isZero();
    }

    @Test
    @DisplayName("R5: seat count never exceeds 10 or drops below 0")
    void countBounds() {
        fill(25);
        assertThat(train.getConfirmedCount()).isEqualTo(10);
        assertThat(train.getWaitingCount()).isEqualTo(15);
        assertThat(train.getSeatsSnapshot()).doesNotContainNull();
    }

    @Test
    @DisplayName("R6: booking closes when the train has ARRIVED or DEPARTED")
    void bookingClosed() {
        train.assignPlatform("P2");
        train.markArrived(at("07:00"));
        assertThatThrownBy(() -> train.bookSeat(ticket(), at("07:01")))
                .isInstanceOf(SeatUnavailableException.class).hasMessageContaining("closed");
        train.markDeparted(at("07:15"));
        assertThat(train.acceptsBookings()).isFalse();
        assertThatThrownBy(() -> train.bookSeat(ticket(), at("07:20"))).isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    @DisplayName("R5: tickets of a departed train cannot be cancelled")
    void cannotCancelAfterDeparture() {
        Reservation r = fill(1).get(0);
        train.assignPlatform("P2");
        train.markArrived(at("07:00"));
        train.markDeparted(at("07:15"));
        assertThatThrownBy(() -> train.cancelBooking(r, at("07:30"))).isInstanceOf(InvalidCancellationException.class);
    }

    @Test
    @DisplayName("Train: a ticket for another train is refused")
    void wrongTrain() {
        Reservation other = new Reservation("X", 1, new Passenger("X/1", "A", 30, Gender.MALE), "99999", at("06:00"));
        assertThatThrownBy(() -> train.bookSeat(other, at("06:00"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> train.cancelBooking(other, at("06:00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Train: status transitions are validated")
    void transitions() {
        assertThatThrownBy(() -> train.markArrived(at("07:00"))).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> train.markDeparted(at("07:00"))).isInstanceOf(InvalidOperationException.class);
        train.markRejected();
        assertThat(train.getStatus()).isEqualTo(TrainStatus.REJECTED);
        assertThatThrownBy(() -> train.applyDelay(10, "x")).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Train: delays move both times before arrival, only departure after arrival")
    void delays() {
        train.applyDelay(10, "signal");
        assertThat(train.getExpectedArrival()).isEqualTo(at("07:10"));
        assertThat(train.getDelayReason()).isEqualTo("signal");
        train.assignPlatform("P2");
        train.markArrived(at("07:10"));
        train.applyDelay(5, "loading");
        assertThat(train.getExpectedArrival()).isEqualTo(at("07:10"));
        assertThat(train.getExpectedDeparture()).isEqualTo(at("07:30"));
        assertThat(train.getDelayMinutes()).isEqualTo(15);
        assertThatThrownBy(() -> train.applyDelay(0, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Timetable: a SCHEDULED train can be edited, others cannot")
    void reschedule() {
        train.reschedule(at("08:00"), at("08:20"), Priority.CONNECTION);
        assertThat(train.getScheduledArrival()).isEqualTo(at("08:00"));
        assertThat(train.getDeclaredPriority()).isEqualTo(Priority.CONNECTION);
        assertThatThrownBy(() -> train.reschedule(at("09:00"), at("08:00"), null))
                .isInstanceOf(IllegalArgumentException.class);
        train.assignPlatform("P1");
        assertThatThrownBy(() -> train.reschedule(at("09:00"), at("09:20"), null))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Train: constructor validates its fields")
    void validation() {
        assertThatThrownBy(() -> train("12", "07:00", "07:15", 400, Priority.NORMAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> train("12635", "07:00", "06:15", 400, Priority.NORMAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> train("12635", "07:00", "07:15", 0, Priority.NORMAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(train("12635", "07:00", "07:15", 10, null).getDeclaredPriority()).isEqualTo(Priority.NORMAL);
    }

    @Test
    @DisplayName("Passenger: name, age and gender are validated")
    void passengerValidation() {
        assertThatThrownBy(() -> new Passenger("1", " ", 30, Gender.MALE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Passenger("1", "A", 0, Gender.MALE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Passenger("1", "A", 30, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
