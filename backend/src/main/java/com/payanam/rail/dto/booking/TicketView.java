package com.payanam.rail.dto.booking;

import java.util.List;

/**
 * One passenger of a PNR. {@code name} is null in the public PNR lookup (privacy).
 *
 * @param label ticket-style status: CNF/S7, WL/2 or CAN
 */
public record TicketView(int passengerNo, String name, int age, String gender, String status, String label,
                         Integer seatNo, int waitingPosition, boolean promoted, Integer lastSeatNo,
                         List<TimelineStepView> timeline) {
}
