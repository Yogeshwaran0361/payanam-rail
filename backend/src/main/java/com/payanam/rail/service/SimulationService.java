package com.payanam.rail.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payanam.rail.domain.audit.StationSnapshot;
import com.payanam.rail.domain.policy.AllocationPolicyType;
import com.payanam.rail.dto.station.ClockView;
import com.payanam.rail.dto.station.SettingsView;
import com.payanam.rail.dto.station.SnapshotView;
import com.payanam.rail.repository.StationSnapshotRepository;
import com.payanam.rail.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Simulation Control: the clock (advance / play / pause), seed reload, reset, snapshots and settings. */
@Service
public class SimulationService {
    private final StationService station;
    private final DashboardService dashboard;
    private final OperationsService operations;
    private final StationSnapshotRepository snapshots;
    private final AuditService audit;
    private final ObjectMapper mapper;
    private final Clock clock;

    public SimulationService(StationService station, DashboardService dashboard, OperationsService operations,
                             StationSnapshotRepository snapshots, AuditService audit, ObjectMapper mapper, Clock clock) {
        this.station = station;
        this.dashboard = dashboard;
        this.operations = operations;
        this.snapshots = snapshots;
        this.audit = audit;
        this.mapper = mapper;
        this.clock = clock;
    }

    public ClockView clock() {
        return view(List.of());
    }

    /** +5 / +15 / +30 min buttons: the clock moves and its side effects run (releases, held trains, queue). */
    public ClockView advance(int minutes) {
        List<String> events = station.write(s -> s.advanceClock(minutes));
        audit.record("CLOCK_ADVANCED", "+" + minutes + " min -> " + station.now().toLocalTime());
        return view(events);
    }

    public ClockView setRunning(boolean running) {
        station.run(s -> s.setClockRunning(running));
        audit.record(running ? "CLOCK_PLAY" : "CLOCK_PAUSE", "Clock " + (running ? "playing" : "paused") + " at "
                + station.now().toLocalTime());
        return view(List.of());
    }

    /** The clock cannot run backwards on a live plan, so "reset" restarts the seed day at the start time. */
    public ClockView resetClock() {
        station.loadSeed();
        audit.record("CLOCK_RESET", "Clock reset: seed day reloaded at " + station.now().toLocalTime());
        return view(List.of("Clock reset to " + station.now().toLocalTime() + " with the seed timetable"));
    }

    public ClockView loadSeed() {
        station.loadSeed();
        audit.record("SEED_LOADED", "Seed data loaded (6 trains, 3 platforms, 32 bookings)");
        return view(List.of("Seed data loaded"));
    }

    public ClockView resetSystem() {
        station.resetEmpty();
        audit.record("SYSTEM_RESET", "System reset: all trains and bookings removed, platforms kept");
        return view(List.of("System reset: no trains or bookings"));
    }

    private ClockView view(List<String> events) {
        return station.read(s -> new ClockView(s.now(), s.clock().start(), s.clock().elapsedMinutes(),
                s.clock().isRunning(), station.config().tickSeconds(), station.config().playStepMinutes(), events));
    }

    // ------------------------------------------------------------ snapshots

    /** Saves the KPIs, timetable and platform board as JSON in H2. */
    public SnapshotView saveSnapshot(String label) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("dashboard", dashboard.dashboard());
        content.put("trains", operations.trains());
        content.put("platforms", operations.platforms());
        String json;
        try {
            json = mapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Snapshot could not be serialised", e);
        }
        String by = CurrentUser.get().map(u -> u.email()).orElse("system");
        StationSnapshot saved = snapshots.save(new StationSnapshot(LocalDateTime.now(clock), station.now(), label.trim(),
                by, json));
        audit.record("SNAPSHOT_SAVED", "Snapshot '" + saved.getLabel() + "'");
        return toView(saved);
    }

    public List<SnapshotView> listSnapshots() {
        return snapshots.findTop20ByOrderByCreatedAtDesc().stream().map(SimulationService::toView).toList();
    }

    private static SnapshotView toView(StationSnapshot s) {
        return new SnapshotView(s.getId(), s.getCreatedAt(), s.getSimulationTime(), s.getLabel(), s.getCreatedBy(),
                s.getSummaryJson());
    }

    // ------------------------------------------------------------ settings

    public SettingsView settings() {
        return new SettingsView(station.getPolicyType().name(),
                Arrays.stream(AllocationPolicyType.values()).map(Enum::name).toList(), station.getClearanceBuffer(),
                station.config().name());
    }

    /** P6: switch BestFit / FirstFit at runtime; change the clearance buffer for new allocations. */
    public SettingsView updateSettings(AllocationPolicyType policy, int buffer) {
        station.changeSettings(policy, buffer);
        audit.record("SETTINGS_CHANGED", "Allocation policy " + policy + ", clearance buffer " + buffer + " min");
        return settings();
    }
}
