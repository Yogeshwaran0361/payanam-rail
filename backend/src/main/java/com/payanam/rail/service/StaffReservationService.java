package com.payanam.rail.service;

import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.CancellationOutcome;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.dto.booking.CancellationView;
import com.payanam.rail.dto.station.StaffReservationsView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Reservations Management for booking clerks: a train's seats and FIFO waiting list, manual cancellation
 * (staff override of the ownership rule) and the promotion history.
 */
@Service
public class StaffReservationService {
    private final StationService station;
    private final NotificationService notifications;
    private final AuditService audit;
    private final OperationalPersistenceService operationalPersistence;

    public StaffReservationService(StationService station, NotificationService notifications, AuditService audit,
                                  OperationalPersistenceService operationalPersistence) {
        this.station = station;
        this.notifications = notifications;
        this.audit = audit;
        this.operationalPersistence = operationalPersistence;
    }

    public StaffReservationsView forTrain(String trainNo) {
        return station.read(s -> {
            TrainService t = s.trains().get(trainNo);
            List<StaffReservationsView.SeatRow> seats = new ArrayList<>();
            List<Reservation> snapshot = t.getSeatsSnapshot();
            for (int i = 0; i < snapshot.size(); i++) {
                Reservation r = snapshot.get(i);
                if (r != null) {
                    Booking b = s.reservations().find(r.getPnr());
                    seats.add(new StaffReservationsView.SeatRow(i + 1, r.getId(), r.getPnr(), r.getPassenger().name(),
                            r.getPassenger().age(), r.getPassenger().gender().name(), r.isPromoted(), b.getOwnerName()));
                }
            }
            List<StaffReservationsView.WaitingRow> waiting = new ArrayList<>();
            int position = 1;
            for (Reservation r : t.getWaitingListSnapshot()) {
                Booking b = s.reservations().find(r.getPnr());
                waiting.add(new StaffReservationsView.WaitingRow(position++, r.getId(), r.getPnr(), r.getPassenger().name(),
                        r.getPassenger().age(), r.getPassenger().gender().name(), r.getBookedAt(), b.getOwnerName()));
            }
            List<StationEvent> promotions = new ArrayList<>();
            for (StationEvent e : s.events().all()) {
                if (e.category() == StationEvent.Category.PROMOTION && t.getTrainNo().equals(e.trainNo())) {
                    promotions.add(e);
                }
            }
            List<StaffReservationsView.PnrRow> pnrs = new ArrayList<>();
            for (Booking b : s.reservations().bookingsOfTrain(t.getTrainNo())) {
                var view = StationViews.booking(s, b, true);
                pnrs.add(new StaffReservationsView.PnrRow(b.getPnr(), b.getOwnerName(), b.getBookedAt(), view.summary(),
                        view.cancellable()));
            }
            return new StaffReservationsView(t.getTrainNo(), t.getName(), t.getStatus().name(), TrainService.CAPACITY,
                    seats, waiting, StationViews.events(promotions), pnrs);
        });
    }

    /** A clerk cancels a whole PNR (e.g. at the counter). The promoted passenger is notified. */
    public CancellationView cancel(String pnr) {
        CancellationView view = station.write(s -> {
            List<CancellationOutcome> outcomes = s.cancelBooking(pnr, null, true);
            Booking b = s.reservations().find(pnr);
            for (CancellationOutcome o : outcomes) {
                operationalPersistence.recordCancellation(b, o, null, true, s.now());
            }
            return BookingService.toView(s, pnr, outcomes);
        });
        audit.record("CLERK_CANCELLED_PNR", view.message());
        notifyOwner(view);
        return view;
    }

    /** A clerk cancels one passenger of a PNR. */
    public CancellationView cancelPassenger(String pnr, int passengerNo) {
        CancellationView view = station.write(s -> {
            CancellationOutcome outcome = s.cancelPassenger(pnr, passengerNo, null, true);
            Booking b = s.reservations().find(pnr);
            operationalPersistence.recordCancellation(b, outcome, null, true, s.now());
            return BookingService.toView(s, pnr, List.of(outcome));
        });
        audit.record("CLERK_CANCELLED_PASSENGER", view.message());
        notifyOwner(view);
        return view;
    }

    private void notifyOwner(CancellationView view) {
        Long owner = station.read(s -> s.reservations().find(view.pnr()).getOwnerUserId());
        notifications.add(owner, "CANCELLATION", "Cancelled by station staff on PNR " + view.pnr(),
                String.join(", ", view.cancelledTickets()) + " cancelled at the booking counter", station.now(),
                view.pnr());
    }
}
