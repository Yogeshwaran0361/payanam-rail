package com.payanam.rail.controller;

import com.payanam.rail.dto.booking.BookingRequest;
import com.payanam.rail.dto.booking.BookingView;
import com.payanam.rail.dto.booking.CancellationView;
import com.payanam.rail.dto.booking.PassengerDashboardView;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Passenger bookings (ROLE_PASSENGER only, see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "bookings", description = "Book, view and cancel tickets (passengers)")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book 1-4 passengers: CNF with a seat, or WL/n when the train is full")
    public BookingView book(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody BookingRequest request) {
        return bookings.book(user, request);
    }

    @GetMapping
    @Operation(summary = "My PNRs, newest first")
    public List<BookingView> mine(@AuthenticationPrincipal AuthUser user) {
        return bookings.myBookings(user);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Passenger dashboard data")
    public PassengerDashboardView dashboard(@AuthenticationPrincipal AuthUser user) {
        return bookings.dashboard(user);
    }

    @GetMapping("/{pnr}")
    @Operation(summary = "One of my PNRs (403 for someone else's)")
    public BookingView one(@AuthenticationPrincipal AuthUser user, @PathVariable String pnr) {
        return bookings.myBooking(user, pnr);
    }

    @PostMapping("/{pnr}/cancel")
    @Operation(summary = "Cancel all passengers; the oldest waiting passenger is promoted into each freed seat")
    public CancellationView cancel(@AuthenticationPrincipal AuthUser user, @PathVariable String pnr) {
        return bookings.cancel(user, pnr);
    }

    @PostMapping("/{pnr}/passengers/{passengerNo}/cancel")
    @Operation(summary = "Partial cancellation of one passenger")
    public CancellationView cancelPassenger(@AuthenticationPrincipal AuthUser user, @PathVariable String pnr,
                                            @PathVariable int passengerNo) {
        return bookings.cancelPassenger(user, pnr, passengerNo);
    }
}
