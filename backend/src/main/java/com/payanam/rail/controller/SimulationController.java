package com.payanam.rail.controller;

import com.payanam.rail.dto.station.AdvanceClockRequest;
import com.payanam.rail.dto.station.ClockView;
import com.payanam.rail.dto.station.SnapshotRequest;
import com.payanam.rail.dto.station.SnapshotView;
import com.payanam.rail.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Simulation clock and dataset control. */
@RestController
@RequestMapping("/api/v1/simulation")
@Tag(name = "simulation", description = "Clock, seed data, reset, snapshots")
public class SimulationController {
    private static final String CONTROL = "hasAuthority('PERM_CONTROL_SIMULATION')";

    private final SimulationService simulation;

    public SimulationController(SimulationService simulation) {
        this.simulation = simulation;
    }

    @GetMapping("/clock")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    @Operation(summary = "Current simulation time")
    public ClockView clock() {
        return simulation.clock();
    }

    @PostMapping("/advance")
    @PreAuthorize(CONTROL)
    @Operation(summary = "Advance the clock by 1-240 minutes")
    public ClockView advance(@Valid @RequestBody AdvanceClockRequest request) {
        return simulation.advance(request.minutes());
    }

    @PostMapping("/play")
    @PreAuthorize(CONTROL)
    public ClockView play() {
        return simulation.setRunning(true);
    }

    @PostMapping("/pause")
    @PreAuthorize(CONTROL)
    public ClockView pause() {
        return simulation.setRunning(false);
    }

    @PostMapping("/reset-clock")
    @PreAuthorize(CONTROL)
    @Operation(summary = "Restart the seed day at the start time")
    public ClockView resetClock() {
        return simulation.resetClock();
    }

    @PostMapping("/load-seed")
    @PreAuthorize(CONTROL)
    @Operation(summary = "Reload the seed dataset")
    public ClockView loadSeed() {
        return simulation.loadSeed();
    }

    @PostMapping("/reset-system")
    @PreAuthorize(CONTROL)
    @Operation(summary = "Remove all trains and bookings (platforms stay)")
    public ClockView resetSystem() {
        return simulation.resetSystem();
    }

    @GetMapping("/snapshots")
    @PreAuthorize(CONTROL)
    public List<SnapshotView> snapshots() {
        return simulation.listSnapshots();
    }

    @PostMapping("/snapshots")
    @PreAuthorize(CONTROL)
    @Operation(summary = "Save the current KPIs and plan as a snapshot (H2)")
    public SnapshotView saveSnapshot(@Valid @RequestBody SnapshotRequest request) {
        return simulation.saveSnapshot(request.label());
    }
}
