package com.payanam.rail.domain.engine;

import java.time.LocalDateTime;

/**
 * Sent to the owner of a PNR when one of its waiting passengers is promoted (rule R3),
 * e.g. "WL/1 → CNF/S7". Turned into an in-app notification by the service layer.
 */
public record PromotionNotice(Long ownerUserId, String pnr, String trainNo, String trainName,
                              String passengerName, String fromLabel, String toLabel, LocalDateTime at) {
}
