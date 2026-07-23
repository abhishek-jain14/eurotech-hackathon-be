package com.qagenie.testbe.report.dto;

import java.time.Instant;

public record RunSnapshotDto(
        Long runId, String suiteName, String environmentName, Instant startedAt, Instant completedAt,
        Long durationSeconds, int totalTests, int passedCount, int failedCount, double passRatePercent
) {}
