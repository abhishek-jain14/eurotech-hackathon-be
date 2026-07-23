package com.qagenie.testbe.coverage.dto;

import java.util.List;

/** Platform-wide coverage summary (top stat strip) plus one row per application. */
public record CoverageOverviewDto(
        int totalEndpoints, int coveredEndpoints, int endpointCoveragePercent,
        int totalScenarios, int lastRunPassed, int lastRunFailed, int gapsToFix,
        List<ApplicationCoverageDto> applications
) {}
