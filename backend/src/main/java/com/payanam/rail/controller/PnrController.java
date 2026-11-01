package com.payanam.rail.controller;

import com.payanam.rail.dto.booking.BookingView;
import com.payanam.rail.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public PNR status lookup (no sign-in; passenger names are hidden). */
@RestController
@RequestMapping("/api/v1/pnr")
@Tag(name = "pnr", description = "Public PNR status")
public class PnrController {
    private final BookingService bookings;

    public PnrController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/{pnr}")
    @Operation(summary = "Status and timeline of every passenger on a PNR (404 for an unknown PNR)")
    public BookingView status(@PathVariable String pnr) {
        return bookings.publicStatus(pnr);
    }
}
