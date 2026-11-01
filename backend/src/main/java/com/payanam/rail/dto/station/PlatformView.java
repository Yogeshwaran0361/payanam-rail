package com.payanam.rail.dto.station;

import java.time.LocalDateTime;
import java.util.List;

/** A platform card plus its timeline blocks for the Gantt chart. */
public record PlatformView(String id, String name, int lengthMeters, String status, boolean maintenance,
                           String currentTrainNo, String currentTrainName, LocalDateTime currentStart,
                           LocalDateTime currentEnd, LocalDateTime nextFreeTime, String nextTrainNo,
                           LocalDateTime nextTrainArrival, List<BlockView> blocks) {

    /** One occupation interval drawn on the timeline. */
    public record BlockView(String trainNo, String trainName, LocalDateTime start, LocalDateTime end,
                            String trainStatus, String priority, int lengthMeters) {
    }
}
