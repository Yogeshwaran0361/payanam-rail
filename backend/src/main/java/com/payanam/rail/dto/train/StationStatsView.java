package com.payanam.rail.dto.train;

import java.time.LocalDateTime;

/** Public numbers for the landing page, plus the simulation time shown in every portal's top bar. */
public record StationStatsView(String stationName, int platforms, int trains, int bookings, int occupancyPercent,
                               int seatsLeft, LocalDateTime now) {
}
