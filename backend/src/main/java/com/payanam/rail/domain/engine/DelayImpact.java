package com.payanam.rail.domain.engine;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything a delay changed (rule P7). Shown in the "impact preview" modal before staff confirm,
 * and again after the delay is applied.
 *
 * @param occupationExtended  true if the train kept its platform with a longer interval
 * @param requeued            true if a WAITING train was removed and reinserted in the PriorityQueue
 * @param lostPlatform        true if the train itself had to give up its platform
 * @param bumped              later trains that were un-assigned, with their new allocation result
 * @param alerts              one alert message per affected train
 */
public record DelayImpact(String trainNo, int minutes, String reason, String platformId,
                          LocalDateTime oldArrival, LocalDateTime oldDeparture,
                          LocalDateTime newArrival, LocalDateTime newDeparture,
                          LocalDateTime oldOccupationEnd, LocalDateTime newOccupationEnd,
                          boolean occupationExtended, boolean requeued, boolean lostPlatform,
                          List<AllocationResult> bumped, List<AllocationResult> reallocated,
                          List<String> alerts, String message) {
}
