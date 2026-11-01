package com.payanam.rail.service;

import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.OccupationInterval;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.dto.station.DashboardView;
import com.payanam.rail.dto.station.EventView;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Station Control Centre: KPIs, charts and alerts, all computed from the live in-memory station. */
@Service
public class DashboardService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private final StationService station;

    public DashboardService(StationService station) {
        this.station = station;
    }

    public DashboardView dashboard() {
        return station.read(this::build);
    }

    private DashboardView build(RailwayStation s) {
        int onPlatform = 0, rejected = 0, delayed = 0, departed = 0, confirmed = 0, waitlisted = 0;
        List<DashboardView.SeatOccupancy> seats = new ArrayList<>();
        for (TrainService t : s.trains().timetable()) {
            switch (t.getStatus()) {
                case ARRIVED -> onPlatform++;
                case REJECTED -> rejected++;
                case DEPARTED -> departed++;
                default -> { }
            }
            if (t.getDelayMinutes() > 0 && t.getStatus() != TrainStatus.DEPARTED) {
                delayed++;
            }
            confirmed += t.getConfirmedCount();
            waitlisted += t.getWaitingCount();
            seats.add(new DashboardView.SeatOccupancy(t.getTrainNo(), t.getName(), t.getConfirmedCount(),
                    t.getWaitingCount(), TrainService.CAPACITY));
        }
        int trains = s.trains().size();
        int occupancy = trains == 0 ? 0 : Math.round(100f * confirmed / (trains * TrainService.CAPACITY));
        List<Booking> history = s.reservations().history();
        DashboardView.Kpis kpis = new DashboardView.Kpis(trains, onPlatform, s.platforms().queueSize(), rejected,
                delayed, history.size(), confirmed + waitlisted, occupancy, departed, waitlisted);

        List<EventView> alerts = new ArrayList<>();
        List<StationEvent> all = s.events().all();
        for (int i = all.size() - 1; i >= 0 && alerts.size() < 12; i--) {
            if (all.get(i).alert()) {
                alerts.add(StationViews.event(all.get(i)));
            }
        }
        return new DashboardView(s.now(), kpis, seats, utilisation(s), bookingsOverTime(s, history), alerts,
                StationViews.nextRelease(s));
    }

    /**
     * Platform utilisation = minutes covered by occupation intervals inside the service window
     * (clock start to clock start + 3 h) divided by the window length.
     */
    private List<DashboardView.PlatformUtilisation> utilisation(RailwayStation s) {
        LocalDateTime from = s.clock().start();
        LocalDateTime to = from.plusHours(3);
        long window = Duration.between(from, to).toMinutes();
        List<DashboardView.PlatformUtilisation> result = new ArrayList<>();
        for (Platform p : s.platforms().sortedPlatforms()) {
            long minutes = 0;
            for (OccupationInterval i : p.getOccupations()) {
                LocalDateTime a = i.start().isBefore(from) ? from : i.start();
                LocalDateTime b = i.end().isAfter(to) ? to : i.end();
                if (b.isAfter(a)) {
                    minutes += Duration.between(a, b).toMinutes();
                }
            }
            result.add(new DashboardView.PlatformUtilisation(p.getId(), (int) Math.round(100.0 * minutes / window),
                    (int) minutes));
        }
        return result;
    }

    /** Bookings per day for the last 7 days of the service day (history is already sorted by time). */
    private List<DashboardView.BookingsPoint> bookingsOverTime(RailwayStation s, List<Booking> history) {
        LocalDate last = s.now().toLocalDate();
        Map<LocalDate, int[]> perDay = new LinkedHashMap<>();
        for (int d = 6; d >= 0; d--) {
            perDay.put(last.minusDays(d), new int[2]);
        }
        for (Booking b : history) {
            int[] counts = perDay.get(b.getBookedAt().toLocalDate());
            if (counts != null) {
                counts[0]++;
                counts[1] += b.getReservations().size();
            }
        }
        List<DashboardView.BookingsPoint> points = new ArrayList<>();
        perDay.forEach((day, c) -> points.add(new DashboardView.BookingsPoint(day.format(DAY), c[0], c[1])));
        return points;
    }

    /** All station events, newest first (for the event feed). */
    public List<EventView> events(int limit) {
        return station.read(s -> {
            List<StationEvent> all = s.events().all();
            List<EventView> views = new ArrayList<>();
            for (int i = all.size() - 1; i >= 0 && views.size() < limit; i--) {
                views.add(StationViews.event(all.get(i)));
            }
            return views;
        });
    }
}
