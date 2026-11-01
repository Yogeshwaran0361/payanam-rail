package com.payanam.rail.dto.train;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A train as shown in the timetable and on operations screens.
 *
 * @param liveStatus human text, e.g. "On time", "Delayed 15 min", "On platform P2"
 */
public record TrainView(String trainNo, String name, String origin, String destination,
                        LocalDateTime scheduledArrival, LocalDateTime scheduledDeparture,
                        LocalDateTime expectedArrival, LocalDateTime expectedDeparture,
                        int lengthMeters, String priority, String status, String liveStatus, int delayMinutes,
                        String delayReason, String platformId, String requestType, BigDecimal fare, int capacity,
                        int confirmed, int seatsLeft, int waiting, boolean bookingOpen,
                        LocalDateTime actualArrival, LocalDateTime actualDeparture) {
}
