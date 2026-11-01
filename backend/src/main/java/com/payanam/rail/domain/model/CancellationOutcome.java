package com.payanam.rail.domain.model;

/**
 * Result of cancelling one passenger: which seat was freed (null for a waitlisted ticket) and
 * which waiting passenger, if any, was promoted into it (rule R3).
 *
 * @param previousLabel the promoted ticket's label before promotion, e.g. "WL/1"
 */
public record CancellationOutcome(Reservation cancelled, Integer freedSeat, Reservation promoted,
                                  String previousLabel) {

    public boolean hasPromotion() {
        return promoted != null;
    }
}
