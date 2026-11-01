package com.payanam.rail.service;

import com.payanam.rail.domain.engine.AllocationResult;
import com.payanam.rail.domain.engine.DelayImpact;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.PlatformRelease;
import com.payanam.rail.domain.model.PlatformRequest;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.dto.booking.BookingView;
import com.payanam.rail.dto.booking.TicketView;
import com.payanam.rail.dto.booking.TimelineStepView;
import com.payanam.rail.dto.station.AllocationView;
import com.payanam.rail.dto.station.DelayImpactView;
import com.payanam.rail.dto.station.EventView;
import com.payanam.rail.dto.station.QueueEntryView;
import com.payanam.rail.dto.train.TrainView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Maps domain objects to DTOs. The API never returns domain objects directly, so the internal model
 * can change without breaking the frontend, and private data (other passengers' names) never leaks.
 */
public final class StationViews {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private StationViews() {
    }

    public static TrainView train(RailwayStation s, TrainService t) {
        String requestType = s.platforms().requestOf(t.getTrainNo()).map(r -> r.getType().name()).orElse(null);
        return new TrainView(t.getTrainNo(), t.getName(), t.getOrigin(), t.getDestination(), t.getScheduledArrival(),
                t.getScheduledDeparture(), t.getExpectedArrival(), t.getExpectedDeparture(), t.getLengthMeters(),
                t.getDeclaredPriority().name(), t.getStatus().name(), liveStatus(s, t), t.getDelayMinutes(),
                t.getDelayReason(), t.getPlatformId(), requestType, t.getFare(), TrainService.CAPACITY,
                t.getConfirmedCount(), t.getSeatsLeft(), t.getWaitingCount(), t.acceptsBookings(),
                t.getActualArrival(), t.getActualDeparture());
    }

    /** Human-readable live status for passengers and staff. */
    public static String liveStatus(RailwayStation s, TrainService t) {
        String delay = t.getDelayMinutes() > 0 ? " · delayed " + t.getDelayMinutes() + " min" : "";
        return switch (t.getStatus()) {
            case SCHEDULED -> (t.getDelayMinutes() > 0 ? "Delayed " + t.getDelayMinutes() + " min" : "On time");
            case WAITING_FOR_PLATFORM -> (s.now().isBefore(t.getExpectedArrival()) ? "Waiting for a platform"
                    : "Held at signal, waiting for a platform") + delay;
            case PLATFORM_ASSIGNED -> "Platform " + t.getPlatformId() + " assigned" + delay;
            case ARRIVED -> "On platform " + t.getPlatformId() + delay;
            case DEPARTED -> "Departed at " + t.getActualDeparture().format(HM);
            case REJECTED -> "Not calling: overlong for every platform";
        };
    }

    public static BookingView booking(RailwayStation s, Booking b, boolean withNames) {
        TrainService t = s.trains().get(b.getTrainNo());
        List<TicketView> tickets = new ArrayList<>();
        int cnf = 0, wl = 0, can = 0;
        for (int i = 0; i < b.getReservations().size(); i++) {
            Reservation r = b.getReservations().get(i);
            switch (r.getStatus()) {
                case CNF -> cnf++;
                case WL -> wl++;
                case CAN -> can++;
            }
            tickets.add(ticket(i + 1, r, withNames));
        }
        String state = b.isFullyCancelled() ? "CANCELLED"
                : t.getStatus() == TrainStatus.DEPARTED ? "COMPLETED" : "UPCOMING";
        List<String> parts = new ArrayList<>();
        if (cnf > 0) {
            parts.add(cnf + " CNF");
        }
        if (wl > 0) {
            parts.add(wl + " WL");
        }
        if (can > 0) {
            parts.add(can + " CAN");
        }
        boolean cancellable = state.equals("UPCOMING");
        return new BookingView(b.getPnr(), t.getTrainNo(), t.getName(), t.getOrigin(), t.getDestination(),
                t.getExpectedArrival(), t.getExpectedDeparture(), t.getPlatformId(), t.getStatus().name(),
                t.getDelayMinutes(), b.getBookedAt(), withNames ? b.getOwnerName() : null, b.getFarePerPassenger(),
                b.totalFare(), state, String.join(", ", parts), cancellable, tickets);
    }

    public static TicketView ticket(int passengerNo, Reservation r, boolean withName) {
        List<TimelineStepView> timeline = r.getTimeline().stream()
                .map(c -> new TimelineStepView(c.at(), c.label(), c.note())).toList();
        return new TicketView(passengerNo, withName ? r.getPassenger().name() : null, r.getPassenger().age(),
                r.getPassenger().gender().name(), r.getStatus().name(), r.getStatusLabel(), r.getSeatNo(),
                r.getWaitingPosition(), r.isPromoted(), r.getLastSeatNo(), timeline);
    }

    public static AllocationView allocation(AllocationResult r) {
        return new AllocationView(r.trainNo(), r.outcome().name(), r.platformId(), r.start(), r.end(),
                r.queuePosition(), r.message());
    }

    public static List<AllocationView> allocations(List<AllocationResult> list) {
        return list.stream().map(StationViews::allocation).toList();
    }

    public static DelayImpactView delay(DelayImpact d, boolean preview) {
        return new DelayImpactView(preview, d.trainNo(), d.minutes(), d.reason(), d.platformId(), d.oldArrival(),
                d.oldDeparture(), d.newArrival(), d.newDeparture(), d.oldOccupationEnd(), d.newOccupationEnd(),
                d.occupationExtended(), d.requeued(), d.lostPlatform(), allocations(d.bumped()),
                allocations(d.reallocated()), d.alerts(), d.message());
    }

    public static EventView event(StationEvent e) {
        return new EventView(e.sequence(), e.time(), e.category().name(), e.alert(), e.trainNo(), e.message());
    }

    public static List<EventView> events(List<StationEvent> list) {
        return list.stream().map(StationViews::event).toList();
    }

    /** The live PriorityQueue in poll() order, with the min-heap's next compatible release per train. */
    public static List<QueueEntryView> queue(RailwayStation s) {
        List<QueueEntryView> rows = new ArrayList<>();
        int rank = 1;
        for (PlatformRequest r : s.platforms().queueInOrder()) {
            TrainService t = r.getTrain();
            Optional<PlatformRelease> next = s.platforms().earliestCompatibleRelease(t);
            rows.add(new QueueEntryView(rank++, t.getTrainNo(), t.getName(), r.getType().name(), r.priorityRank(),
                    t.getExpectedArrival(), t.getExpectedDeparture().plusMinutes(s.platforms().getClearanceBufferMinutes()),
                    t.getLengthMeters(), r.getRequestedAt(), t.getDelayMinutes(), s.platforms().waitingReason(r),
                    next.map(PlatformRelease::platformId).orElse(null),
                    next.map(PlatformRelease::releaseTime).orElse(null)));
        }
        return rows;
    }

    /** "P2 at 07:25 (after 12635)" or "-". */
    public static String nextRelease(RailwayStation s) {
        return s.platforms().nextRelease().map(r -> r.platformId() + " at " + r.releaseTime().format(HM)
                + " (after " + r.trainNo() + ")").orElse("-");
    }
}
