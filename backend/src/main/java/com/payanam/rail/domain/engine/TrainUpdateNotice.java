package com.payanam.rail.domain.engine;

import java.time.LocalDateTime;

/**
 * Sent to the owner of each active PNR on a train when the control room changes something the
 * passenger should know: a platform assignment or change, a delay, the train being held or arriving.
 */
public record TrainUpdateNotice(Long ownerUserId, String pnr, String trainNo, String trainName, String kind,
                                String message, LocalDateTime at) {
}
