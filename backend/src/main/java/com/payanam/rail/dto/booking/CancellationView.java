package com.payanam.rail.dto.booking;

import java.util.List;

/** What a cancellation did: which tickets were cancelled and who was promoted (rule R3). */
public record CancellationView(String pnr, List<String> cancelledTickets, List<PromotionView> promotions,
                               String message, BookingView booking) {

    /** A waiting passenger moved into the freed seat, e.g. 4000824633/1 WL/1 → CNF/S7. */
    public record PromotionView(String ticketId, String pnr, String from, String to) {
    }
}
