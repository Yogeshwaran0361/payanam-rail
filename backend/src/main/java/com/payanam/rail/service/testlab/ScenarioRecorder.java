package com.payanam.rail.service.testlab;

import com.payanam.rail.dto.testlab.ScenarioResult;
import com.payanam.rail.exception.RailwayException;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the steps of one lab scenario. {@link #check} records an assertion without stopping, so the
 * demo shows every step; the scenario passes only if all checks pass.
 */
public class ScenarioRecorder {
    private final List<ScenarioResult.Step> steps = new ArrayList<>();
    private String actual = "";

    /** Records an action that ran (always ok). */
    public void step(String description) {
        steps.add(new ScenarioResult.Step(description, true));
    }

    /** Records a check and its outcome. */
    public boolean check(String description, boolean ok) {
        steps.add(new ScenarioResult.Step(description, ok));
        return ok;
    }

    /** Runs an action that MUST be refused with the given exception type; returns the error message. */
    public String expectError(String description, Class<? extends RailwayException> type, Runnable action) {
        try {
            action.run();
            check(description + ": no error was raised", false);
            return "no error";
        } catch (RailwayException e) {
            boolean ok = type.isInstance(e);
            check(description + " → " + e.getClass().getSimpleName() + " (" + e.getCode() + "): " + e.getMessage(), ok);
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public void actual(String text) {
        this.actual = text;
    }

    String actual() {
        return actual;
    }

    List<ScenarioResult.Step> steps() {
        return steps;
    }

    boolean allPassed() {
        return !steps.isEmpty() && steps.stream().allMatch(ScenarioResult.Step::ok);
    }
}
