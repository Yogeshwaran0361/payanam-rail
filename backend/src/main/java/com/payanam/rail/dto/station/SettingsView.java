package com.payanam.rail.dto.station;

import java.util.List;

/** Station Settings: allocation policy and clearance buffer. */
public record SettingsView(String allocationPolicy, List<String> availablePolicies, int clearanceBufferMinutes,
                           String stationName) {
}
