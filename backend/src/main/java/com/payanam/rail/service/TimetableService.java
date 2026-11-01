package com.payanam.rail.service;

import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.ReservationStatus;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.dto.train.NewTrainRequest;
import com.payanam.rail.dto.train.SeatMapView;
import com.payanam.rail.dto.train.StationStatsView;
import com.payanam.rail.dto.train.TrainEditRequest;
import com.payanam.rail.dto.train.TrainView;
import com.payanam.rail.security.AuthUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The timetable (sorted list), train details, seat map, live status and timetable management. */
@Service
public class TimetableService {
    private final StationService station;
    private final AuditService audit;

    public TimetableService(StationService station, AuditService audit) {
        this.station = station;
        this.audit = audit;
    }

    /** Trains in arrival order (the registry's sorted ArrayList, no sorting needed here). */
    public List<TrainView> timetable() {
        return station.read(s -> s.trains().timetable().stream().map(t -> StationViews.train(s, t)).toList());
    }

    public TrainView train(String trainNo) {
        return station.read(s -> StationViews.train(s, s.trains().get(trainNo)));
    }

    /**
     * The 10-seat grid. Seats are marked "mine" only for the signed-in passenger's own tickets;
     * no other names are ever sent to the browser.
     */
    public SeatMapView seatMap(String trainNo, AuthUser viewer) {
        return station.read(s -> {
            TrainService t = s.trains().get(trainNo);
            Set<String> myTickets = new HashSet<>();
            List<Integer> myWaiting = new ArrayList<>();
            if (viewer != null && !viewer.isStaff()) {
                for (Booking b : s.reservations().bookingsOf(viewer.id())) {
                    if (b.getTrainNo().equals(t.getTrainNo())) {
                        for (Reservation r : b.getReservations()) {
                            myTickets.add(r.getId());
                            if (r.getStatus() == ReservationStatus.WL) {
                                myWaiting.add(r.getWaitingPosition());
                            }
                        }
                    }
                }
            }
            List<SeatMapView.SeatView> seats = new ArrayList<>();
            List<Reservation> snapshot = t.getSeatsSnapshot();
            for (int i = 0; i < snapshot.size(); i++) {
                Reservation r = snapshot.get(i);
                seats.add(new SeatMapView.SeatView(i + 1, r != null, r != null && myTickets.contains(r.getId())));
            }
            myWaiting.sort(Integer::compareTo);
            return new SeatMapView(t.getTrainNo(), t.getName(), TrainService.CAPACITY, seats, t.getWaitingCount(),
                    myWaiting, t.acceptsBookings());
        });
    }

    /** Landing-page numbers. */
    public StationStatsView stats() {
        return station.read(s -> {
            int confirmed = 0, seatsLeft = 0;
            for (TrainService t : s.trains().timetable()) {
                confirmed += t.getConfirmedCount();
                if (t.acceptsBookings()) {
                    seatsLeft += t.getSeatsLeft();
                }
            }
            int trains = s.trains().size();
            int occupancy = trains == 0 ? 0 : Math.round(100f * confirmed / (trains * TrainService.CAPACITY));
            return new StationStatsView(s.getName(), s.platforms().sortedPlatforms().size(), trains,
                    s.reservations().history().size(), occupancy, seatsLeft, s.now());
        });
    }

    /** Timetable Management: edit times and priority of a SCHEDULED train. */
    public TrainView edit(String trainNo, TrainEditRequest r) {
        TrainView view = station.write(s -> {
            LocalDate day = s.clock().start().toLocalDate();
            TrainService t = s.editTrain(trainNo, day.atTime(r.arrival()), day.atTime(r.departure()), r.priority());
            return StationViews.train(s, t);
        });
        audit.record("TRAIN_EDITED", trainNo + ": " + r.arrival() + "-" + r.departure() + ", " + r.priority());
        return view;
    }

    /** Timetable Management: add a service (at most 6 in total). */
    public TrainView add(NewTrainRequest r) {
        TrainView view = station.write(s -> {
            LocalDate day = s.clock().start().toLocalDate();
            TrainService t = new TrainService(r.trainNo(), r.name(), r.origin(), r.destination(),
                    day.atTime(r.arrival()), day.atTime(r.departure()), r.lengthMeters(), r.priority(), r.fare());
            s.addTrain(t);
            return StationViews.train(s, t);
        });
        audit.record("TRAIN_ADDED", r.trainNo() + " " + r.name());
        return view;
    }
}
