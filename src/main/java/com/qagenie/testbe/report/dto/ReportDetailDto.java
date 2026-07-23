package com.qagenie.testbe.report.dto;

import java.util.List;

/** The rich, latest-run-focused report view for one application (stats, donut, comparison, flakiness, failures). */
public record ReportDetailDto(
        RunSnapshotDto latestRun, RunSnapshotDto previousRun,
        int skippedCount, int uncoveredEndpoints, int partialEndpoints,
        List<FlakyScenarioDto> flakyScenarios, List<FailureDetailDto> failures
) {}
