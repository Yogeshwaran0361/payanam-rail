package com.payanam.rail.dto.testlab;

import java.util.List;

/** Outcome of one lab run: PASS/FAIL, each checked step, and expected vs actual in plain words. */
public record ScenarioResult(String id, String title, String rule, String kind, boolean passed, String expected,
                             String actual, List<Step> steps, long durationMs) {

    /** One action or check inside the scenario. */
    public record Step(String description, boolean ok) {
    }
}
