package com.qagenie.testbe.coverage.dto;

import java.util.List;

/** Coverage for a single endpoint within an application - status is one of NO_TESTS, PARTIAL, FAILURES, FULL. */
public record EndpointCoverageDto(
        String httpMethod, String path, String summary,
        int positiveCount, int negativeCount, int passedCount, int failedCount,
        boolean hasFlow, boolean hasTestData, String status,
        List<Long> missingDataScenarioIds
) {}
