package com.payanam.rail.service;

import com.payanam.rail.domain.engine.PassengerDetails;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.CancellationOutcome;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.ReservationStatus;
import com.payanam.rail.domain.model.StatusChange;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.dto.booking.BookingRequest;
import com.payanam.rail.dto.booking.BookingView;
import com.payanam.rail.dto.booking.CancellationView;
import com.payanam.rail.dto.booking.NotificationView;
import com.payanam.rail.dto.booking.PassengerDashboardView;
import com.payanam.rail.security.AuthUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Passenger-side reservations: book, list, cancel (whole PNR or one passenger), public PNR status and
 * the passenger dashboard. Business rules are enforced by the domain (TrainService, ReservationDesk);
 * this class adds the signed-in user, DTO mapping, notifications and the audit trail.
 */
@Service
public class BookingService {
    private final StationService station;
    private final NotificationService notifications;
    private final AuditService audit;
    private final OperationalPersistenceService operationalPersistence;

    public BookingService(StationService station, NotificationService notifications, AuditService audit,
                          OperationalPersistenceService operationalPersistence) {
        this.station = station;
        this.notifications = notifications;
        this.audit = audit;
        this.operationalPersistence = operationalPersistence;
    }

    /** R1, R2: one PNR, 1–4 passengers, each CNF or WL. */
    public BookingView book(AuthUser user, BookingRequest request) {
        List<PassengerDetails> people = request.passengers().stream()
                .map(p -> new PassengerDetails(p.name(), p.age(), p.gender())).toList();
        BookingView view = station.write(s -> {
            Booking b = s.book(user.id(), user.fullName(), request.trainNo(), people);
            operationalPersistence.saveBooking(b);
            return StationViews.booking(s, b, true);
        });
        notifications.add(user.id(), "BOOKING", "PNR " + view.pnr() + " booked",
                view.trainNo() + " " + view.trainName() + ": " + view.summary(), view.bookedAt(), view.pnr());
        audit.record("BOOKING_CREATED", "PNR " + view.pnr() + " on " + view.trainNo() + " - " + view.summary());
        return view;
    }

    public List<BookingView> myBookings(AuthUser user) {
        return station.read(s -> s.reservations().bookingsOf(user.id()).stream()
                .map(b -> StationViews.booking(s, b, true)).toList());
    }

    /** R5: only the owner can open the full PNR (with names). */
    public BookingView myBooking(AuthUser user, String pnr) {
        return station.read(s -> StationViews.booking(s, s.reservations().findOwned(pnr, user.id()), true));
    }

    /** Public PNR status: statuses and timeline per passenger, but no names (privacy). */
    public BookingView publicStatus(String pnr) {
        return station.read(s -> StationViews.booking(s, s.reservations().find(pnr), false));
    }

    /** Counter lookup for staff: any PNR, with names. */
    public BookingView staffView(String pnr) {
        return station.read(s -> StationViews.booking(s, s.reservations().find(pnr), true));
    }

    /** R3, R5: cancel every active passenger of the caller's own PNR. */
    public CancellationView cancel(AuthUser user, String pnr) {
        CancellationView view = station.write(s -> {
            List<CancellationOutcome> outcomes = s.cancelBooking(pnr, user.id(), false);
            Booking b = s.reservations().find(pnr);
            for (CancellationOutcome o : outcomes) {
                operationalPersistence.recordCancellation(b, o, user.id(), false, s.now());
            }
            return toView(s, pnr, outcomes);
        });
        audit.record("BOOKING_CANCELLED", view.message());
        notifyCancelled(user, view);
        return view;
    }

    /** R4: cancel one passenger of the caller's own PNR. */
    public CancellationView cancelPassenger(AuthUser user, String pnr, int passengerNo) {
        CancellationView view = station.write(s -> {
            CancellationOutcome outcome = s.cancelPassenger(pnr, passengerNo, user.id(), false);
            Booking b = s.reservations().find(pnr);
            operationalPersistence.recordCancellation(b, outcome, user.id(), false, s.now());
            return toView(s, pnr, List.of(outcome));
        });
        audit.record("PASSENGER_CANCELLED", view.message());
        notifyCancelled(user, view);
        return view;
    }

    private void notifyCancelled(AuthUser user, CancellationView view) {
        notifications.add(user.id(), "CANCELLATION", "Cancelled on PNR " + view.pnr(),
                String.join(", ", view.cancelledTickets()) + " cancelled", station.now(), view.pnr());
    }

    /** Builds the result shared by passenger and clerk cancellations. */
    static CancellationView toView(RailwayStation s, String pnr, List<CancellationOutcome> outcomes) {
        List<String> cancelled = new ArrayList<>();
        List<CancellationView.PromotionView> promotions = new ArrayList<>();
        for (CancellationOutcome o : outcomes) {
            String seat = o.freedSeat() == null ? "WL" : "S" + o.freedSeat();
            cancelled.add(o.cancelled().getId() + " (" + seat + ")");
            if (o.hasPromotion()) {
                promotions.add(new CancellationView.PromotionView(o.promoted().getId(), o.promoted().getPnr(),
                        o.previousLabel(), o.promoted().getStatusLabel()));
            }
        }
        StringBuilder message = new StringBuilder("Cancelled " + String.join(", ", cancelled) + ".");
        for (CancellationView.PromotionView p : promotions) {
            message.append(" Promoted ").append(p.ticketId()).append(": ").append(p.from()).append(" → ")
                    .append(p.to()).append('.');
        }
        Booking booking = s.reservations().find(pnr);
        return new CancellationView(booking.getPnr(), cancelled, promotions, message.toString(),
                StationViews.booking(s, booking, true));
    }

    /** Data of the passenger dashboard. */
    public PassengerDashboardView dashboard(AuthUser user) {
        List<NotificationView> recentNotifications = notifications.forUser(user.id()).stream().limit(5).toList();
        long unread = notifications.unreadCount(user.id());
        return station.read(s -> {
            List<Booking> mine = s.reservations().bookingsOf(user.id());
            int upcoming = 0, cnf = 0, wl = 0, can = 0;
            Booking next = null;
            List<PassengerDashboardView.ActivityView> activity = new ArrayList<>();
            for (Booking b : mine) {
                TrainService t = s.trains().get(b.getTrainNo());
                boolean active = !b.isFullyCancelled() && t.getStatus() != com.payanam.rail.domain.model.TrainStatus.DEPARTED;
                if (active) {
                    upcoming++;
                    if (next == null || t.getExpectedArrival().isBefore(s.trains().get(next.getTrainNo()).getExpectedArrival())) {
                        next = b;
                    }
                }
                for (Reservation r : b.getReservations()) {
                    if (r.getStatus() == ReservationStatus.CAN) {
                        can++;
                    } else if (active && r.getStatus() == ReservationStatus.CNF) {
                        cnf++;
                    } else if (active) {
                        wl++;
                    }
                    for (StatusChange c : r.getTimeline()) {
                        if (!c.label().equals("Booked")) {
                            activity.add(new PassengerDashboardView.ActivityView(c.at(), b.getPnr(), b.getTrainNo(),
                                    r.getPassenger().name() + ": " + c.label() + " - " + c.note()));
                        }
                    }
                }
            }
            activity.sort(Comparator.comparing(PassengerDashboardView.ActivityView::at).reversed());
            return new PassengerDashboardView(user.fullName(), upcoming, cnf, wl, can,
                    next == null ? null : StationViews.booking(s, next, true),
                    activity.stream().limit(8).toList(), recentNotifications, unread);
        });
    }
}
