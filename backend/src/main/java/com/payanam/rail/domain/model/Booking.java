package com.payanam.rail.domain.model;

import com.payanam.rail.exception.ReservationNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A PNR: one booking made by one account for 1–4 passengers on one train (rule R2).
 * Each passenger has their own {@link Reservation} (own seat or waiting-list position), so
 * the PNR shows a status per passenger, like a real ticket.
 */
public class Booking {
    /** R2: one PNR holds between 1 and 4 passengers. */
    public static final int MAX_PASSENGERS = 4;

    private final String pnr;
    private final Long ownerUserId;
    private final String ownerName;
    private final String trainNo;
    private final LocalDateTime bookedAt;
    private final BigDecimal farePerPassenger;
    private final List<Reservation> reservations;

    public Booking(String pnr, Long ownerUserId, String ownerName, String trainNo, LocalDateTime bookedAt,
                   BigDecimal farePerPassenger, List<Reservation> reservations) {
        if (reservations.isEmpty() || reservations.size() > MAX_PASSENGERS) {
            throw new IllegalArgumentException("A PNR must have between 1 and " + MAX_PASSENGERS + " passengers");
        }
        this.pnr = pnr;
        this.ownerUserId = ownerUserId;
        this.ownerName = ownerName;
        this.trainNo = trainNo;
        this.bookedAt = bookedAt;
        this.farePerPassenger = farePerPassenger;
        this.reservations = List.copyOf(reservations);
    }

    /** True if the given account owns this PNR (R5: nobody may change someone else's PNR). */
    public boolean isOwnedBy(Long userId) {
        return ownerUserId != null && ownerUserId.equals(userId);
    }

    /** The ticket of passenger number {@code passengerNo} (1-based, as printed on the ticket). */
    public Reservation passenger(int passengerNo) {
        if (passengerNo < 1 || passengerNo > reservations.size()) {
            throw new ReservationNotFoundException("PNR " + pnr + " has no passenger " + passengerNo);
        }
        return reservations.get(passengerNo - 1);
    }

    /** Passengers whose ticket is still CNF or WL. */
    public List<Reservation> activeReservations() {
        List<Reservation> active = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.isActive()) {
                active.add(r);
            }
        }
        return active;
    }

    public boolean isFullyCancelled() {
        return activeReservations().isEmpty();
    }

    /** Fare of the tickets still active (cancelled ones no longer count). Information only: no payment gateway. */
    public BigDecimal totalFare() {
        return farePerPassenger.multiply(BigDecimal.valueOf(activeReservations().size()));
    }

    public String getPnr() {
        return pnr;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public BigDecimal getFarePerPassenger() {
        return farePerPassenger;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }
}
