package com.payanam.rail.domain.model;

import com.payanam.rail.exception.InvalidCancellationException;
import com.payanam.rail.exception.InvalidOperationException;
import com.payanam.rail.exception.SeatUnavailableException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A train service calling at Payanam Junction, with one seat class of exactly 10 seats and one
 * end-to-end booking segment.
 *
 * <p>COMPOSITION: a TrainService OWNS its confirmed tickets (the {@code seats} array) and its FIFO
 * waiting list. Both are created with the train, are never shared with another train and can only
 * change through {@link #bookSeat} and {@link #cancelBooking}.</p>
 *
 * <p>ENCAPSULATION: all fields are private and there are no rule-breaking setters. The seat-count
 * invariant (0 &lt;= confirmed &lt;= 10) and every status transition are validated inside the methods.</p>
 */
public class TrainService {
    /** Fixed assumption: one seat class with exactly 10 seats. */
    public static final int CAPACITY = 10;

    private final String trainNo;
    private final String name;
    private final String origin;
    private final String destination;
    private final int lengthMeters;
    private final BigDecimal fare;

    // Timetable values. Editable by staff only while the train is still SCHEDULED.
    private LocalDateTime scheduledArrival;
    private LocalDateTime scheduledDeparture;
    private Priority declaredPriority;

    // Times after delays. The platform interval is always computed from these.
    private LocalDateTime expectedArrival;
    private LocalDateTime expectedDeparture;
    private int delayMinutes;
    private String delayReason;
    private TrainStatus status = TrainStatus.SCHEDULED;
    private String platformId;
    private LocalDateTime actualArrival;
    private LocalDateTime actualDeparture;

    /** seats[i] is the ticket in seat (i + 1), or null if that seat is free. */
    private final Reservation[] seats = new Reservation[CAPACITY];
    private int confirmedCount;

    /*
     * ArrayDeque = FIFO waiting list. WHY: the rule is "the OLDEST waiting passenger is promoted first".
     * addLast() and pollFirst() are O(1). An ArrayList would need an O(n) shift for every removal at the
     * front, and a LinkedList allocates one node object per passenger.
     */
    private final ArrayDeque<Reservation> waitingList = new ArrayDeque<>();

    public TrainService(String trainNo, String name, String origin, String destination,
                        LocalDateTime scheduledArrival, LocalDateTime scheduledDeparture,
                        int lengthMeters, Priority declaredPriority, BigDecimal fare) {
        if (trainNo == null || !trainNo.trim().matches("\\d{5}")) {
            throw new IllegalArgumentException("Train number must have 5 digits");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Train name is required");
        }
        if (lengthMeters <= 0) {
            throw new IllegalArgumentException("Train length must be positive for train " + trainNo);
        }
        if (fare == null || fare.signum() < 0) {
            throw new IllegalArgumentException("Fare cannot be negative for train " + trainNo);
        }
        requireDepartureAfterArrival(trainNo, scheduledArrival, scheduledDeparture);
        this.trainNo = trainNo.trim();
        this.name = name.trim();
        this.origin = origin;
        this.destination = destination;
        this.scheduledArrival = scheduledArrival;
        this.scheduledDeparture = scheduledDeparture;
        this.lengthMeters = lengthMeters;
        this.declaredPriority = declaredPriority == null ? Priority.NORMAL : declaredPriority;
        this.fare = fare;
        this.expectedArrival = scheduledArrival;
        this.expectedDeparture = scheduledDeparture;
    }

    /**
     * Deep copy for "what-if" previews. {@code copies} maps every original ticket to its copy so the
     * copied seats and waiting list point at the copied tickets (and not at the live ones).
     */
    public TrainService copy(Map<Reservation, Reservation> copies) {
        TrainService c = new TrainService(trainNo, name, origin, destination, scheduledArrival, scheduledDeparture,
                lengthMeters, declaredPriority, fare);
        c.expectedArrival = expectedArrival;
        c.expectedDeparture = expectedDeparture;
        c.delayMinutes = delayMinutes;
        c.delayReason = delayReason;
        c.status = status;
        c.platformId = platformId;
        c.actualArrival = actualArrival;
        c.actualDeparture = actualDeparture;
        c.confirmedCount = confirmedCount;
        for (int i = 0; i < CAPACITY; i++) {
            c.seats[i] = seats[i] == null ? null : copies.computeIfAbsent(seats[i], Reservation::new);
        }
        for (Reservation r : waitingList) {
            c.waitingList.addLast(copies.computeIfAbsent(r, Reservation::new));
        }
        return c;
    }

    /** Helper for callers that need an identity map for {@link #copy}. */
    public static Map<Reservation, Reservation> newCopyMap() {
        return new IdentityHashMap<>();
    }

    /** Copies a ticket that is not seated on any train any more (e.g. a cancelled one). */
    public static Reservation copyTicket(Reservation original, Map<Reservation, Reservation> copies) {
        return copies.computeIfAbsent(original, Reservation::new);
    }

    // ================================================================== reservations (R1–R6)

    /**
     * R1: if fewer than 10 seats are confirmed the ticket gets the lowest free seat (CNF);
     * otherwise it joins the BACK of the FIFO waiting list (WL/n).
     * R6: refused once the train has arrived or departed.
     */
    public ReservationStatus bookSeat(Reservation ticket, LocalDateTime when) {
        if (!trainNo.equals(ticket.getTrainNo())) {
            throw new IllegalArgumentException("Ticket " + ticket.getId() + " is not for train " + trainNo);
        }
        if (!status.bookingOpen()) {
            throw new SeatUnavailableException("Booking is closed for train " + trainNo + " (" + status + ")");
        }
        if (confirmedCount < CAPACITY) {
            int seat = lowestFreeSeat();
            seats[seat - 1] = ticket;
            incrementConfirmed();
            ticket.confirm(seat, when);
        } else {
            waitingList.addLast(ticket);
            ticket.waitlist(waitingList.size(), when);
        }
        return ticket.getStatus();
    }

    /**
     * R3: cancelling a confirmed ticket frees its seat and IMMEDIATELY promotes the oldest waiting
     * passenger (FIFO head) into that exact seat. Cancelling a waitlisted ticket only removes it from
     * the queue. R5: a second cancellation of the same ticket is rejected.
     */
    public CancellationOutcome cancelBooking(Reservation ticket, LocalDateTime when) {
        if (!trainNo.equals(ticket.getTrainNo())) {
            throw new IllegalArgumentException("Ticket " + ticket.getId() + " is not for train " + trainNo);
        }
        if (ticket.getStatus() == ReservationStatus.CAN) {
            throw new InvalidCancellationException("Ticket " + ticket.getId() + " is already cancelled");
        }
        if (status == TrainStatus.DEPARTED) {
            throw new InvalidCancellationException("Train " + trainNo + " has departed; its tickets cannot be cancelled");
        }
        if (ticket.getStatus() == ReservationStatus.WL) {
            waitingList.remove(ticket);           // O(n) scan, n = waiting passengers (small)
            ticket.cancel(when);
            renumberWaitingList(when);
            return new CancellationOutcome(ticket, null, null, null);
        }
        int seat = ticket.getSeatNo();
        if (seats[seat - 1] != ticket) {
            throw new IllegalStateException("Seat " + seat + " of train " + trainNo + " is not held by " + ticket.getId());
        }
        seats[seat - 1] = null;
        decrementConfirmed();
        ticket.cancel(when);

        Reservation promoted = waitingList.pollFirst();   // FIFO: oldest waiting passenger, O(1)
        String previousLabel = null;
        if (promoted != null) {
            previousLabel = promoted.getStatusLabel();
            seats[seat - 1] = promoted;                     // the EXACT seat that was freed
            incrementConfirmed();
            promoted.promote(seat, when);
            renumberWaitingList(when);
        }
        return new CancellationOutcome(ticket, seat, promoted, previousLabel);
    }

    /** After someone leaves the queue, everyone behind moves up one place (WL/3 → WL/2). */
    private void renumberWaitingList(LocalDateTime when) {
        int position = 1;
        for (Reservation r : waitingList) {
            r.moveInQueue(position++, when);
        }
    }

    private int lowestFreeSeat() {
        for (int i = 0; i < CAPACITY; i++) {
            if (seats[i] == null) {
                return i + 1;
            }
        }
        throw new SeatUnavailableException("Train " + trainNo + " has no free seat");
    }

    /** R5: the confirmed count can never exceed 10. */
    private void incrementConfirmed() {
        if (confirmedCount >= CAPACITY) {
            throw new SeatUnavailableException("Train " + trainNo + " cannot exceed " + CAPACITY + " seats");
        }
        confirmedCount++;
    }

    /** R5: the confirmed count can never drop below 0. */
    private void decrementConfirmed() {
        if (confirmedCount <= 0) {
            throw new SeatUnavailableException("Train " + trainNo + " seat count cannot go below 0");
        }
        confirmedCount--;
    }

    // ================================================================== timetable & status

    /** Timetable edit by staff. Only allowed before the train has asked for a platform. */
    public void reschedule(LocalDateTime arrival, LocalDateTime departure, Priority priority) {
        if (status != TrainStatus.SCHEDULED) {
            throw new InvalidOperationException("Train " + trainNo + " is " + status
                    + "; only SCHEDULED trains can be edited (report a delay instead)");
        }
        requireDepartureAfterArrival(trainNo, arrival, departure);
        int shift = (int) java.time.Duration.between(scheduledArrival, expectedArrival).toMinutes();
        scheduledArrival = arrival;
        scheduledDeparture = departure;
        expectedArrival = arrival.plusMinutes(shift);
        expectedDeparture = departure.plusMinutes(shift);
        if (priority != null) {
            declaredPriority = priority;
        }
    }

    /**
     * Applies a delay. Before arrival both times move; once the train is on the platform only its
     * departure moves. Callers that keep the train inside a sorted structure must REMOVE it first and
     * REINSERT it afterwards, because its sort key changes here.
     */
    public void applyDelay(int minutes, String reason) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Delay must be a positive number of minutes");
        }
        if (status == TrainStatus.DEPARTED || status == TrainStatus.REJECTED) {
            throw new InvalidOperationException("Train " + trainNo + " is " + status + "; a delay cannot be applied");
        }
        if (status != TrainStatus.ARRIVED) {
            expectedArrival = expectedArrival.plusMinutes(minutes);
        }
        expectedDeparture = expectedDeparture.plusMinutes(minutes);
        delayMinutes += minutes;
        delayReason = reason;
    }

    public void markWaiting() {
        requireStatus("wait for a platform", TrainStatus.SCHEDULED, TrainStatus.PLATFORM_ASSIGNED,
                TrainStatus.WAITING_FOR_PLATFORM);
        status = TrainStatus.WAITING_FOR_PLATFORM;
        platformId = null;
    }

    public void assignPlatform(String newPlatformId) {
        requireStatus("be assigned a platform", TrainStatus.SCHEDULED, TrainStatus.WAITING_FOR_PLATFORM);
        status = TrainStatus.PLATFORM_ASSIGNED;
        platformId = newPlatformId;
    }

    public void markRejected() {
        requireStatus("be rejected", TrainStatus.SCHEDULED);
        status = TrainStatus.REJECTED;
    }

    public void markArrived(LocalDateTime when) {
        requireStatus("arrive", TrainStatus.PLATFORM_ASSIGNED);
        status = TrainStatus.ARRIVED;
        actualArrival = when;
    }

    public void markDeparted(LocalDateTime when) {
        requireStatus("depart", TrainStatus.ARRIVED);
        status = TrainStatus.DEPARTED;
        actualDeparture = when;
        expectedDeparture = when;
    }

    private void requireStatus(String action, TrainStatus... allowed) {
        for (TrainStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new InvalidOperationException("Train " + trainNo + " cannot " + action + " while " + status);
    }

    private static void requireDepartureAfterArrival(String trainNo, LocalDateTime arrival, LocalDateTime departure) {
        if (arrival == null || departure == null || !departure.isAfter(arrival)) {
            throw new IllegalArgumentException("Departure must be after arrival for train " + trainNo);
        }
    }

    // ================================================================== read-only getters

    public String getTrainNo() {
        return trainNo;
    }

    public String getName() {
        return name;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public LocalDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public LocalDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public LocalDateTime getExpectedArrival() {
        return expectedArrival;
    }

    public LocalDateTime getExpectedDeparture() {
        return expectedDeparture;
    }

    public int getLengthMeters() {
        return lengthMeters;
    }

    public Priority getDeclaredPriority() {
        return declaredPriority;
    }

    public TrainStatus getStatus() {
        return status;
    }

    public String getPlatformId() {
        return platformId;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public LocalDateTime getActualArrival() {
        return actualArrival;
    }

    public LocalDateTime getActualDeparture() {
        return actualDeparture;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public int getSeatsLeft() {
        return CAPACITY - confirmedCount;
    }

    public int getWaitingCount() {
        return waitingList.size();
    }

    public boolean acceptsBookings() {
        return status.bookingOpen();
    }

    /** Copy of the seat array (index 0 = seat 1). Callers cannot change the real seats. */
    public List<Reservation> getSeatsSnapshot() {
        List<Reservation> copy = new ArrayList<>(CAPACITY);
        Collections.addAll(copy, seats);
        return copy;
    }

    /** Waiting list in FIFO order (head = next to be promoted). Read-only copy. */
    public List<Reservation> getWaitingListSnapshot() {
        return List.copyOf(waitingList);
    }
}
