package com.payanam.rail.controller;

import com.payanam.rail.dto.station.MaintenanceRequest;
import com.payanam.rail.dto.station.OperationView;
import com.payanam.rail.dto.station.PlatformBoardView;
import com.payanam.rail.dto.station.PlatformEditRequest;
import com.payanam.rail.dto.station.PlatformView;
import com.payanam.rail.dto.station.QueueEntryView;
import com.payanam.rail.service.OperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Platform board, PriorityQueue view and platform management (staff only). */
@RestController
@RequestMapping("/api/v1/platforms")
@Tag(name = "platforms", description = "Platform board, waiting queue, maintenance")
public class PlatformController {
    private final OperationsService operations;

    public PlatformController(OperationsService operations) {
        this.operations = operations;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_MANAGE_PLATFORMS', 'PERM_MANAGE_TRAINS')")
    @Operation(summary = "Platform cards with status, current train and next free time")
    public List<PlatformView> platforms() {
        return operations.platforms();
    }

    @GetMapping("/board")
    @PreAuthorize("hasAnyAuthority('PERM_MANAGE_PLATFORMS', 'PERM_MANAGE_TRAINS')")
    @Operation(summary = "Gantt data: platforms, intervals, waiting trains and the 'now' line")
    public PlatformBoardView board() {
        return operations.board();
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyAuthority('PERM_MANAGE_PLATFORMS', 'PERM_MANAGE_TRAINS')")
    @Operation(summary = "The waiting PriorityQueue in poll() order")
    public List<QueueEntryView> queue() {
        return operations.queue();
    }

    @PutMapping("/{platformId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_PLATFORMS')")
    @Operation(summary = "Change the platform length (never below a train planned on it)")
    public OperationView edit(@PathVariable String platformId, @Valid @RequestBody PlatformEditRequest request) {
        return operations.changeLength(platformId, request.lengthMeters());
    }

    @PostMapping("/{platformId}/maintenance")
    @PreAuthorize("hasAuthority('PERM_MANAGE_PLATFORMS')")
    @Operation(summary = "P9: maintenance on/off; planned trains are re-allocated, an arrived train stays")
    public OperationView maintenance(@PathVariable String platformId, @RequestBody MaintenanceRequest request) {
        return operations.setMaintenance(platformId, request.maintenance());
    }
}
