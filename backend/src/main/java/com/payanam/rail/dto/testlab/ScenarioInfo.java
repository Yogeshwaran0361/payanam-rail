package com.payanam.rail.dto.testlab;

/** One Test Scenarios Lab entry as listed before running. kind = POSITIVE (T1–T7) or FAILURE (F1–F4). */
public record ScenarioInfo(String id, String title, String rule, String kind, String description) {
}
