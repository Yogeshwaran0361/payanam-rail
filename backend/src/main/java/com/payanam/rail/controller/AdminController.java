package com.payanam.rail.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payanam.rail.dto.station.SettingsUpdateRequest;
import com.payanam.rail.dto.station.SettingsView;
import com.payanam.rail.service.SimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "admin", description = "Station settings")
public class AdminController {
    private final SimulationService simulation;

    public AdminController(SimulationService simulation) {
        this.simulation = simulation;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public SettingsView settings() {
        return simulation.settings();
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('PERM_MANAGE_SETTINGS')")
    @Operation(summary = "P6: switch the allocation policy; change the buffer for new allocations")
    public SettingsView update(@Valid @RequestBody SettingsUpdateRequest request) {
        return simulation.updateSettings(request.allocationPolicy(), request.clearanceBufferMinutes());
    }
}
