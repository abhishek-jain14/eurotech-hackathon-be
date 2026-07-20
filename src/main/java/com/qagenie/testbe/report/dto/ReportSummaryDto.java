package com.qagenie.testbe.report.dto;

public record ReportSummaryDto(
        Long applicationId,
        long totalRuns,
        long totalScenariosExecuted,
        long totalPassed,
        long totalFailed,
        double passRatePercent,
        double avgDurationSeconds
) {}
