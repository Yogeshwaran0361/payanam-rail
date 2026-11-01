package com.payanam.rail.dto.station;

import java.time.LocalDateTime;
import java.util.List;

/** Station Control Centre: KPI cards, three charts and the active alerts. */
public record DashboardView(LocalDateTime now, Kpis kpis, List<SeatOccupancy> seatOccupancy,
                            List<PlatformUtilisation> platformUtilisation, List<BookingsPoint> bookingsOverTime,
                            List<EventView> alerts, String nextRelease) {

    /** The KPI cards. */
    public record Kpis(int trainsToday, int onPlatform, int waitingForPlatform, int rejected, int delayed,
                       int totalBookings, int activeTickets, int occupancyPercent, int departed, int waitlisted) {
    }

    /** Seats per train for the occupancy chart. */
    public record SeatOccupancy(String trainNo, String trainName, int confirmed, int waiting, int capacity) {
    }

    /** Share of the service window during which a platform is planned or occupied. */
    public record PlatformUtilisation(String platformId, int percent, int plannedMinutes) {
    }

    /** Bookings per day. */
    public record BookingsPoint(String label, int bookings, int tickets) {
    }
}
