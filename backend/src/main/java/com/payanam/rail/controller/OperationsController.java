package com.payanam.rail.controller;

import com.payanam.rail.dto.station.AllocationView;
import com.payanam.rail.dto.station.ArrivalRequestBody;
import com.payanam.rail.dto.station.DashboardView;
import com.payanam.rail.dto.station.DelayImpactView;
import com.payanam.rail.dto.station.DelayRequest;
import com.payanam.rail.dto.station.EventView;
import com.payanam.rail.dto.station.OperationView;
import com.payanam.rail.dto.train.TrainView;
import com.payanam.rail.service.DashboardService;
import com.payanam.rail.service.OperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Train operations: arrival requests, confirm arrival / departure, report delay. Each state change has
 * a "/preview" twin that runs the same operation on a copy of the station and changes nothing.
 */
@RestController
@RequestMapping("/api/v1/operations")
@Tag(name = "operations", description = "Control Centre, arrival requests, arrivals, departures, delays")
public class OperationsController {
    private static final String TRAIN_OPS = "hasAnyAuthority('PERM_MANAGE_TRAINS', 'PERM_MANAGE_PLATFORMS')";

    private final OperationsService operations;
    private final DashboardService dashboard;

    public OperationsController(OperationsService operations, DashboardService dashboard) {
        this.operations = operations;
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    @Operation(summary = "Control Centre KPIs, charts and alerts")
    public DashboardView dashboard() {
        return dashboard.dashboard();
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    @Operation(summary = "Station event feed (simulation time), newest first")
    public List<EventView> events(@RequestParam(defaultValue = "100") int limit) {
        return dashboard.events(Math.min(Math.max(limit, 1), 500));
    }

    @GetMapping("/trains")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    @Operation(summary = "All trains with operational details")
    public List<TrainView> trains() {
        return operations.trains();
    }

    @PostMapping("/arrival-requests")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "Request a platform: ASSIGNED, QUEUED or REJECTED")
    public AllocationView arrivalRequest(@Valid @RequestBody ArrivalRequestBody request) {
        return operations.requestPlatform(request.trainNo(), request.type());
    }

    @PostMapping("/trains/{trainNo}/arrival")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "Confirm arrival (clock must have reached the expected arrival)")
    public OperationView arrival(@PathVariable String trainNo) {
        return operations.confirmArrival(trainNo, false);
    }

    @PostMapping("/trains/{trainNo}/arrival/preview")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "Preview of a confirmed arrival (nothing changes)")
    public OperationView arrivalPreview(@PathVariable String trainNo) {
        return operations.confirmArrival(trainNo, true);
    }

    @PostMapping("/trains/{trainNo}/departure")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "P8: confirm departure; platform released at departure + buffer, queue allocated")
    public OperationView departure(@PathVariable String trainNo) {
        return operations.confirmDeparture(trainNo, false);
    }

    @PostMapping("/trains/{trainNo}/departure/preview")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "Preview of a confirmed departure (nothing changes)")
    public OperationView departurePreview(@PathVariable String trainNo) {
        return operations.confirmDeparture(trainNo, true);
    }

    @PostMapping("/trains/{trainNo}/delay")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "P7: report a delay; later overlapping trains are re-queued and re-allocated")
    public DelayImpactView delay(@PathVariable String trainNo, @Valid @RequestBody DelayRequest request) {
        return operations.reportDelay(trainNo, request.minutes(), request.reason(), false);
    }

    @PostMapping("/trains/{trainNo}/delay/preview")
    @PreAuthorize(TRAIN_OPS)
    @Operation(summary = "Impact preview of a delay (nothing changes)")
    public DelayImpactView delayPreview(@PathVariable String trainNo, @Valid @RequestBody DelayRequest request) {
        return operations.reportDelay(trainNo, request.minutes(), request.reason(), true);
    }
}
