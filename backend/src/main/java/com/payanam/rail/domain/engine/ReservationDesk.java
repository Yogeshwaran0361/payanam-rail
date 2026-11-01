package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.CancellationOutcome;
import com.payanam.rail.domain.model.Passenger;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.SortedLists;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.exception.InvalidCancellationException;
import com.payanam.rail.exception.ReservationNotFoundException;
import com.payanam.rail.exception.SeatUnavailableException;
import com.payanam.rail.exception.UnauthorizedActionException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reservations of the station (rules R1–R6). Seat and waiting-list logic itself lives in
 * {@link TrainService}; the desk creates PNRs, finds them, checks ownership and records history.
 */
public class ReservationDesk {
    /** Reservation history order: booking time, then PNR (deterministic). */
    private static final Comparator<Booking> HISTORY_ORDER =
            Comparator.comparing(Booking::getBookedAt).thenComparing(Booking::getPnr);

    private final TrainRegistry registry;
    private final EventLog log;
    private StationListener listener = StationListener.NONE;

    /* HashMap PNR → Booking: O(1) PNR lookup (PNR status page, cancellations). */
    private final Map<String, Booking> bookingsByPnr = new HashMap<>();
    /* HashMap passenger id ("PNR/n") → Passenger: O(1) lookup of a traveller. */
    private final Map<String, Passenger> passengersById = new HashMap<>();
    /* HashMap account id → that account's PNRs: O(1) "My Bookings". */
    private final Map<Long, List<String>> pnrsByOwner = new HashMap<>();
    /*
     * Sorted ArrayList of all bookings by booking time (binary-search insert). Seed bookings arrive in
     * any order with past timestamps, but "bookings over time" and history screens read them in order.
     */
    private final List<Booking> history = new ArrayList<>();

    private long pnrCounter;

    public ReservationDesk(TrainRegistry registry, EventLog log) {
        this.registry = registry;
        this.log = log;
    }

    void setListener(StationListener listener) {
        this.listener = listener;
    }

    /**
     * Books 1–4 passengers on one train under one new PNR (R1, R2). Each passenger is passed to
     * {@link TrainService#bookSeat} in order, so the first passengers get the last free seats and the
     * rest go to the waiting list.
     */
    public Booking book(Long ownerUserId, String ownerName, String trainNo, List<PassengerDetails> people,
                        LocalDateTime when) {
        if (people == null || people.isEmpty() || people.size() > Booking.MAX_PASSENGERS) {
            throw new IllegalArgumentException("A booking must have between 1 and " + Booking.MAX_PASSENGERS
                    + " passengers");
        }
        TrainService train = registry.get(trainNo);
        if (!train.acceptsBookings()) {
            throw new SeatUnavailableException("Booking is closed for train " + train.getTrainNo() + " ("
                    + train.getStatus() + ")");
        }
        String pnr = nextPnr();
        List<Reservation> tickets = new ArrayList<>();
        for (int i = 0; i < people.size(); i++) {
            PassengerDetails p = people.get(i);
            Passenger passenger = new Passenger(pnr + "/" + (i + 1), p.name(), p.age(), p.gender());
            tickets.add(new Reservation(pnr, i + 1, passenger, train.getTrainNo(), when));
        }
        // Validate everything before touching the train, so a bad passenger cannot leave half a booking.
        for (Reservation ticket : tickets) {
            train.bookSeat(ticket, when);
            passengersById.put(ticket.getPassenger().id(), ticket.getPassenger());
        }
        Booking booking = new Booking(pnr, ownerUserId, ownerName, train.getTrainNo(), when, train.getFare(), tickets);
        bookingsByPnr.put(pnr, booking);
        pnrsByOwner.computeIfAbsent(ownerUserId, id -> new ArrayList<>()).add(pnr);
        SortedLists.insertSorted(history, booking, HISTORY_ORDER);
        log.record(when, StationEvent.Category.BOOKING, false, train.getTrainNo(), "PNR " + pnr + " booked on "
                + train.getTrainNo() + ": " + labels(tickets));
        return booking;
    }

    /** Cancels every active passenger of a PNR (R3, R5). */
    public List<CancellationOutcome> cancelBooking(String pnr, Long actorUserId, boolean staffOverride,
                                                   LocalDateTime when) {
        Booking booking = findForChange(pnr, actorUserId, staffOverride);
        List<Reservation> active = booking.activeReservations();
        if (active.isEmpty()) {
            throw new InvalidCancellationException("PNR " + booking.getPnr() + " is already fully cancelled");
        }
        List<CancellationOutcome> outcomes = new ArrayList<>();
        for (Reservation ticket : active) {
            outcomes.add(cancelTicket(booking, ticket, when));
        }
        return outcomes;
    }

    /** Partial cancellation: cancels one passenger of a PNR (R4). */
    public CancellationOutcome cancelPassenger(String pnr, int passengerNo, Long actorUserId, boolean staffOverride,
                                               LocalDateTime when) {
        Booking booking = findForChange(pnr, actorUserId, staffOverride);
        return cancelTicket(booking, booking.passenger(passengerNo), when);
    }

    private CancellationOutcome cancelTicket(Booking booking, Reservation ticket, LocalDateTime when) {
        TrainService train = registry.get(booking.getTrainNo());
        String before = ticket.getStatusLabel();
        CancellationOutcome outcome = train.cancelBooking(ticket, when);
        log.record(when, StationEvent.Category.CANCELLATION, false, train.getTrainNo(),
                "Ticket " + ticket.getId() + " (" + before + ") cancelled on " + train.getTrainNo());
        if (outcome.hasPromotion()) {
            Reservation promoted = outcome.promoted();
            Booking promotedBooking = bookingsByPnr.get(promoted.getPnr());
            log.record(when, StationEvent.Category.PROMOTION, false, train.getTrainNo(),
                    "Promoted " + promoted.getId() + ": " + outcome.previousLabel() + " -> " + promoted.getStatusLabel());
            listener.onPromotion(new PromotionNotice(promotedBooking.getOwnerUserId(), promoted.getPnr(),
                    train.getTrainNo(), train.getName(), promoted.getPassenger().name(), outcome.previousLabel(),
                    promoted.getStatusLabel(), when));
        }
        return outcome;
    }

    /** Finds a PNR and checks that the actor may change it (R5: never someone else's PNR). */
    private Booking findForChange(String pnr, Long actorUserId, boolean staffOverride) {
        Booking booking = find(pnr);
        if (!staffOverride && !booking.isOwnedBy(actorUserId)) {
            throw new UnauthorizedActionException("PNR " + booking.getPnr() + " belongs to another account");
        }
        return booking;
    }

    /** O(1) PNR lookup; an unknown PNR raises ReservationNotFoundException (test F1). */
    public Booking find(String pnr) {
        Booking booking = pnr == null ? null : bookingsByPnr.get(pnr.trim());
        if (booking == null) {
            throw new ReservationNotFoundException("PNR " + pnr + " was not found");
        }
        return booking;
    }

    /** Returns the PNR only if the given account owns it. */
    public Booking findOwned(String pnr, Long ownerUserId) {
        Booking booking = find(pnr);
        if (!booking.isOwnedBy(ownerUserId)) {
            throw new UnauthorizedActionException("PNR " + booking.getPnr() + " belongs to another account");
        }
        return booking;
    }

    public Passenger findPassenger(String passengerId) {
        Passenger p = passengersById.get(passengerId);
        if (p == null) {
            throw new ReservationNotFoundException("Passenger " + passengerId + " was not found");
        }
        return p;
    }

    /** The PNRs of one account, newest first. */
    public List<Booking> bookingsOf(Long ownerUserId) {
        List<Booking> result = new ArrayList<>();
        for (String pnr : pnrsByOwner.getOrDefault(ownerUserId, List.of())) {
            result.add(bookingsByPnr.get(pnr));
        }
        result.sort(HISTORY_ORDER.reversed());
        return result;
    }

    /** All PNRs of one train, in booking order. */
    public List<Booking> bookingsOfTrain(String trainNo) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : history) {
            if (b.getTrainNo().equals(trainNo)) {
                result.add(b);
            }
        }
        return result;
    }

    /** Every booking in booking-time order (read-only). */
    public List<Booking> history() {
        return List.copyOf(history);
    }

    /**
     * PNR generator: 10 digits, "4" + a scrambled running number. Multiplying by 824633 (a prime that
     * shares no factor with 10^9) modulo 10^9 is a one-to-one mapping, so PNRs are unique and
     * deterministic but do not look sequential (they cannot be guessed by adding 1).
     */
    private String nextPnr() {
        pnrCounter++;
        long scrambled = (pnrCounter * 824_633L) % 1_000_000_000L;
        return "4" + String.format("%09d", scrambled);
    }

    private static String labels(List<Reservation> tickets) {
        List<String> parts = new ArrayList<>();
        for (Reservation r : tickets) {
            parts.add(r.getPassenger().name() + " " + r.getStatusLabel());
        }
        return String.join(", ", parts);
    }

    ReservationDesk copy(TrainRegistry copiedRegistry, EventLog copiedLog, Map<Reservation, Reservation> ticketCopies) {
        ReservationDesk c = new ReservationDesk(copiedRegistry, copiedLog);
        c.pnrCounter = pnrCounter;
        c.passengersById.putAll(passengersById);
        for (Booking b : history) {
            List<Reservation> tickets = new ArrayList<>();
            for (Reservation r : b.getReservations()) {
                tickets.add(TrainService.copyTicket(r, ticketCopies));
            }
            Booking copy = new Booking(b.getPnr(), b.getOwnerUserId(), b.getOwnerName(), b.getTrainNo(),
                    b.getBookedAt(), b.getFarePerPassenger(), tickets);
            c.bookingsByPnr.put(copy.getPnr(), copy);
            c.pnrsByOwner.computeIfAbsent(copy.getOwnerUserId(), id -> new ArrayList<>()).add(copy.getPnr());
            c.history.add(copy);
        }
        return c;
    }
}
