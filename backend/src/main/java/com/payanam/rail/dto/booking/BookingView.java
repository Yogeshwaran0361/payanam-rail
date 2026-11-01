package com.payanam.rail.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A PNR with its train and every passenger's status.
 *
 * @param journeyState UPCOMING, COMPLETED (train departed) or CANCELLED (all passengers cancelled)
 * @param summary      e.g. "2 CNF, 1 WL"
 */
public record BookingView(String pnr, String trainNo, String trainName, String origin, String destination,
                          LocalDateTime arrival, LocalDateTime departure, String platformId, String trainStatus,
                          int delayMinutes, LocalDateTime bookedAt, String bookedBy, BigDecimal farePerPassenger,
                          BigDecimal totalFare, String journeyState, String summary, boolean cancellable,
                          List<TicketView> passengers) {
}
