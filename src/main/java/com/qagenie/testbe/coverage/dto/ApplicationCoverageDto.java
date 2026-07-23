package com.qagenie.testbe.coverage.dto;

/** Application-level coverage rollup - status is one of UNCOVERED, PARTIAL, FAILURES, GOOD. */
public record ApplicationCoverageDto(
        Long applicationId, String applicationName, String applicationType, String specFormat,
        int totalEndpoints, int coveredEndpoints, int coveragePercent,
        int totalScenarios, int passedCount, int failedCount, int gapsCount, String status
) {}
