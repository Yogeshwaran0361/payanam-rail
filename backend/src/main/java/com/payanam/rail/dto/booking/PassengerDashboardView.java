package com.payanam.rail.dto.booking;

import java.util.List;

/** Data of the passenger dashboard: KPI cards, next journey, recent activity, notifications. */
public record PassengerDashboardView(String fullName, int upcomingJourneys, int confirmedTickets,
                                     int waitlistedTickets, int cancelledTickets, BookingView nextJourney,
                                     List<ActivityView> recentActivity, List<NotificationView> notifications,
                                     long unreadNotifications) {

    /** One line of "recent activity". */
    public record ActivityView(java.time.LocalDateTime at, String pnr, String trainNo, String text) {
    }
}
