package com.qagenie.testbe.report.dto;

/** A scenario with mixed pass/fail results across its last runs - genuinely flaky, not just failing. */
public record FlakyScenarioDto(
        Long scenarioId, String scenarioName, int totalRuns, int passedRuns, int failedRuns, double stabilityPercent
) {}
