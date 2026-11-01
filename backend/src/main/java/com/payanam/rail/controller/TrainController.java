package com.payanam.rail.controller;

import com.payanam.rail.dto.train.NewTrainRequest;
import com.payanam.rail.dto.train.SeatMapView;
import com.payanam.rail.dto.train.StationStatsView;
import com.payanam.rail.dto.train.TrainEditRequest;
import com.payanam.rail.dto.train.TrainView;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Timetable (public), train details, seat map, live status and timetable management (staff). */
@RestController
@RequestMapping("/api/v1/trains")
@Tag(name = "trains", description = "Timetable, seat map, live status, timetable management")
public class TrainController {
    private final TimetableService timetable;

    public TrainController(TimetableService timetable) {
        this.timetable = timetable;
    }

    @GetMapping
    @Operation(summary = "Public timetable, sorted by expected arrival")
    public List<TrainView> timetable() {
        return timetable.timetable();
    }

    @GetMapping("/stats")
    @Operation(summary = "Public station numbers for the landing page")
    public StationStatsView stats() {
        return timetable.stats();
    }

    @GetMapping("/{trainNo}")
    @Operation(summary = "One train")
    public TrainView train(@PathVariable String trainNo) {
        return timetable.train(trainNo);
    }

    @GetMapping("/{trainNo}/live")
    @Operation(summary = "Public live status of a train")
    public TrainView live(@PathVariable String trainNo) {
        return timetable.train(trainNo);
    }

    @GetMapping("/{trainNo}/seat-map")
    @Operation(summary = "10-seat grid without names; seats of the caller's own tickets are marked")
    public SeatMapView seatMap(@PathVariable String trainNo, @AuthenticationPrincipal AuthUser user) {
        return timetable.seatMap(trainNo, user);
    }

    @PutMapping("/{trainNo}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_TRAINS')")
    @Operation(summary = "Edit times / priority of a SCHEDULED train")
    public TrainView edit(@PathVariable String trainNo, @Valid @RequestBody TrainEditRequest request) {
        return timetable.edit(trainNo, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_MANAGE_TRAINS')")
    @Operation(summary = "Add a train (at most 6 services)")
    public TrainView add(@Valid @RequestBody NewTrainRequest request) {
        return timetable.add(request);
    }
}
