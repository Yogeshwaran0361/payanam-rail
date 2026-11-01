package com.payanam.rail.controller;

import com.payanam.rail.dto.testlab.ScenarioInfo;
import com.payanam.rail.dto.testlab.ScenarioResult;
import com.payanam.rail.service.testlab.TestScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Test Scenarios Lab: run T1–T7 and F1–F4, each on an isolated dataset (never the live station). */
@RestController
@RequestMapping("/api/v1/tests")
@Tag(name = "tests", description = "Test Scenarios Lab")
@PreAuthorize("hasAuthority('PERM_RUN_TESTS')")
public class TestLabController {
    private final TestScenarioService lab;

    public TestLabController(TestScenarioService lab) {
        this.lab = lab;
    }

    @GetMapping
    @Operation(summary = "List the lab scenarios")
    public List<ScenarioInfo> list() {
        return lab.list();
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "Run one scenario (T1–T7, F1–F4) on a fresh dataset")
    public ScenarioResult run(@PathVariable String id) {
        return lab.run(id);
    }

    @PostMapping("/run-all")
    @Operation(summary = "Run every scenario")
    public List<ScenarioResult> runAll() {
        return lab.runAll();
    }
}
