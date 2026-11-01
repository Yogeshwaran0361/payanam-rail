package com.payanam.rail.service.testlab;

import java.util.function.Consumer;

/**
 * A runnable demo scenario of the Test Scenarios Lab.
 *
 * @param kind     POSITIVE (a rule working: T1–T7) or FAILURE (a bad request refused: F1–F4)
 * @param expected the expected outcome in plain words, shown next to the actual one
 * @param body     builds its own isolated data and records each step and check
 */
public record LabScenario(String id, String title, String rule, String kind, String description, String expected,
                          Consumer<ScenarioRecorder> body) {
}
