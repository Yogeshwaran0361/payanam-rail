package com.payanam.rail.dto.station;

import java.time.LocalDateTime;
import java.util.List;

/** Reservations Management: one train's seat list, FIFO waiting list and promotion history. */
public record StaffReservationsView(String trainNo, String trainName, String trainStatus, int capacity,
                                    List<SeatRow> confirmed, List<WaitingRow> waiting, List<EventView> promotions,
                                    List<PnrRow> pnrs) {

    /** A seat and who holds it. */
    public record SeatRow(int seatNo, String ticketId, String pnr, String passengerName, int age, String gender,
                          boolean promoted, String bookedBy) {
    }

    /** A waiting passenger in FIFO order (position 1 = next to be promoted). */
    public record WaitingRow(int position, String ticketId, String pnr, String passengerName, int age,
                             String gender, LocalDateTime bookedAt, String bookedBy) {
    }

    /** One PNR of the train. */
    public record PnrRow(String pnr, String bookedBy, LocalDateTime bookedAt, String summary, boolean cancellable) {
    }
}
