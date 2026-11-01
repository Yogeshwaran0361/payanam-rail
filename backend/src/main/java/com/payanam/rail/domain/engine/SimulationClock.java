package com.payanam.rail.domain.engine;

import java.time.LocalDateTime;

/**
 * The station's "current time". Nothing in the domain reads the computer clock: every rule uses
 * this simulated time, so a 3-hour timetable can be demonstrated in a few minutes and tests are
 * deterministic. Staff can advance it, pause it, play it (auto-advance) and reset it.
 */
public class SimulationClock {
    private final LocalDateTime start;
    private LocalDateTime now;
    private boolean running;

    public SimulationClock(LocalDateTime start) {
        this.start = start;
        this.now = start;
    }

    /** Moves the clock forward. The clock never goes backwards (only {@link #reset()} does). */
    public LocalDateTime advance(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("The clock can only move forward");
        }
        now = now.plusMinutes(minutes);
        return now;
    }

    public void reset() {
        now = start;
        running = false;
    }

    SimulationClock copy() {
        SimulationClock c = new SimulationClock(start);
        c.now = now;
        c.running = false;
        return c;
    }

    public LocalDateTime now() {
        return now;
    }

    public LocalDateTime start() {
        return start;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long elapsedMinutes() {
        return java.time.Duration.between(start, now).toMinutes();
    }
}
