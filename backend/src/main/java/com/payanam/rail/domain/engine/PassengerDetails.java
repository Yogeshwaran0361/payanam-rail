package com.payanam.rail.domain.engine;

import com.payanam.rail.domain.model.Gender;

/** Passenger details typed in the booking wizard, before a PNR exists. */
public record PassengerDetails(String name, int age, Gender gender) {
}
