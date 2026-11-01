package com.payanam.rail.dto.train;

import java.util.List;

/**
 * The 10-seat grid. For privacy no names are shown: a seat is only free, taken, or "mine" (held by
 * the signed-in passenger's own PNRs).
 */
public record SeatMapView(String trainNo, String trainName, int capacity, List<SeatView> seats, int waitingCount,
                          List<Integer> myWaitingPositions, boolean bookingOpen) {

    /** One seat. */
    public record SeatView(int seatNo, boolean occupied, boolean mine) {
    }
}
