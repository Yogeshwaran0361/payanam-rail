package com.payanam.rail.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The ticket of ONE passenger on one train (single class, end-to-end segment). A PNR
 * ({@link Booking}) groups 1–4 of these, and each has its own seat or waiting-list position (R2).
 *
 * <p>ENCAPSULATION: every state-changing method is package-private, so only the owning
 * {@link TrainService} (same package) can confirm, waitlist, promote or cancel a ticket.
 * Services and controllers can read the state but can never corrupt it.</p>
 */
public class Reservation {
    private final String id;
    private final String pnr;
    private final Passenger passenger;
    private final String trainNo;
    private final LocalDateTime bookedAt;

    private ReservationStatus status;      // null until the train accepts the ticket
    private Integer seatNo;                // 1..10 while CNF, otherwise null
    private int waitingPosition;           // 1..n while WL, otherwise 0
    private Integer lastSeatNo;            // seat held before cancellation (for display)
    private boolean promoted;              // true if this ticket moved from WL to CNF
    private LocalDateTime cancelledAt;
    private final List<StatusChange> timeline = new ArrayList<>();

    /** Creates an unconfirmed ticket; it gets a status only when passed to {@link TrainService#bookSeat}. */
    public Reservation(String pnr, int passengerNo, Passenger passenger, String trainNo, LocalDateTime bookedAt) {
        this.pnr = pnr;
        this.id = pnr + "/" + passengerNo;
        this.passenger = passenger;
        this.trainNo = trainNo;
        this.bookedAt = bookedAt;
    }

    /** Deep copy used by {@link com.payanam.rail.domain.engine.RailwayStation#copy()} for "what-if" previews. */
    Reservation(Reservation other) {
        this.id = other.id;
        this.pnr = other.pnr;
        this.passenger = other.passenger;
        this.trainNo = other.trainNo;
        this.bookedAt = other.bookedAt;
        this.status = other.status;
        this.seatNo = other.seatNo;
        this.waitingPosition = other.waitingPosition;
        this.lastSeatNo = other.lastSeatNo;
        this.promoted = other.promoted;
        this.cancelledAt = other.cancelledAt;
        this.timeline.addAll(other.timeline);
    }

    void confirm(int seat, LocalDateTime when) {
        requireStatus(null, "confirm");
        status = ReservationStatus.CNF;
        seatNo = seat;
        timeline.add(new StatusChange(when, "Booked", "Booking received"));
        timeline.add(new StatusChange(when, getStatusLabel(), "Seat " + seat + " confirmed"));
    }

    void waitlist(int position, LocalDateTime when) {
        requireStatus(null, "waitlist");
        status = ReservationStatus.WL;
        waitingPosition = position;
        timeline.add(new StatusChange(when, "Booked", "Booking received"));
        timeline.add(new StatusChange(when, getStatusLabel(), "Train full, added to the waiting list"));
    }

    /** Records a move up the waiting list (e.g. WL/2 → WL/1) after someone ahead left the queue. */
    void moveInQueue(int newPosition, LocalDateTime when) {
        requireStatus(ReservationStatus.WL, "move in the queue");
        if (newPosition != waitingPosition) {
            waitingPosition = newPosition;
            timeline.add(new StatusChange(when, getStatusLabel(), "Moved up the waiting list"));
        }
    }

    void promote(int seat, LocalDateTime when) {
        requireStatus(ReservationStatus.WL, "promote");
        String from = getStatusLabel();
        status = ReservationStatus.CNF;
        seatNo = seat;
        waitingPosition = 0;
        promoted = true;
        timeline.add(new StatusChange(when, getStatusLabel(), "Promoted from " + from + " after a cancellation"));
    }

    void cancel(LocalDateTime when) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot cancel ticket " + id + " in status " + status);
        }
        lastSeatNo = seatNo;
        seatNo = null;
        waitingPosition = 0;
        status = ReservationStatus.CAN;
        cancelledAt = when;
        timeline.add(new StatusChange(when, "CAN", "Ticket cancelled"));
    }

    private void requireStatus(ReservationStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Cannot " + action + " ticket " + id + " in status " + status);
        }
    }

    /** Ticket-style label: "CNF/S7", "WL/2" or "CAN". */
    public String getStatusLabel() {
        if (status == null) {
            return "NEW";
        }
        return switch (status) {
            case CNF -> "CNF/S" + seatNo;
            case WL -> "WL/" + waitingPosition;
            case CAN -> "CAN";
        };
    }

    public boolean isActive() {
        return status == ReservationStatus.CNF || status == ReservationStatus.WL;
    }

    public String getId() {
        return id;
    }

    public String getPnr() {
        return pnr;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Integer getSeatNo() {
        return seatNo;
    }

    public int getWaitingPosition() {
        return waitingPosition;
    }

    public Integer getLastSeatNo() {
        return lastSeatNo;
    }

    public boolean isPromoted() {
        return promoted;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    /** Read-only copy of the ticket's timeline, oldest first. */
    public List<StatusChange> getTimeline() {
        return List.copyOf(timeline);
    }
}
