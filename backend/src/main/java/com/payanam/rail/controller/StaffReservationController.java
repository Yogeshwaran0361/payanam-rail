package com.payanam.rail.controller;

import com.payanam.rail.dto.booking.BookingView;
import com.payanam.rail.dto.booking.CancellationView;
import com.payanam.rail.dto.station.StaffReservationsView;
import com.payanam.rail.service.BookingService;
import com.payanam.rail.service.StaffReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reservations Management for booking clerks (and the Station Master). */
@RestController
@RequestMapping("/api/v1/operations/reservations")
@PreAuthorize("hasAuthority('PERM_MANAGE_RESERVATIONS')")
@Tag(name = "operations")
public class StaffReservationController {
    private final StaffReservationService reservations;
    private final BookingService bookings;

    public StaffReservationController(StaffReservationService reservations, BookingService bookings) {
        this.reservations = reservations;
        this.bookings = bookings;
    }

    @GetMapping("/trains/{trainNo}")
    @Operation(summary = "Seats, FIFO waiting list, PNRs and promotion history of one train")
    public StaffReservationsView forTrain(@PathVariable String trainNo) {
        return reservations.forTrain(trainNo);
    }

    @GetMapping("/pnr/{pnr}")
    @Operation(summary = "Any PNR with passenger names (counter lookup)")
    public BookingView pnr(@PathVariable String pnr) {
        return bookings.staffView(pnr);
    }

    @PostMapping("/pnr/{pnr}/cancel")
    @Operation(summary = "Cancel a whole PNR at the counter")
    public CancellationView cancel(@PathVariable String pnr) {
        return reservations.cancel(pnr);
    }

    @PostMapping("/pnr/{pnr}/passengers/{passengerNo}/cancel")
    @Operation(summary = "Cancel one passenger of a PNR at the counter")
    public CancellationView cancelPassenger(@PathVariable String pnr, @PathVariable int passengerNo) {
        return reservations.cancelPassenger(pnr, passengerNo);
    }
}
