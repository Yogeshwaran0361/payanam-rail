package com.payanam.rail.dto.station;

import java.util.List;

/**
 * Generic result of a station operation (arrival, departure, maintenance...), with the station events it
 * caused. {@code preview} = true when it was only simulated on a copy of the station.
 */
public record OperationView(boolean preview, String message, List<AllocationView> allocations, List<String> alerts,
                            List<EventView> events) {
}
