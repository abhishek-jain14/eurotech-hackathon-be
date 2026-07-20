package com.qagenie.testbe.dashboard.dto;

public record DashboardStatsDto(
        double passRatePercent,
        long totalFailures,
        long totalScenarios,
        long totalApplications,
        long totalProjects,
        double avgDurationSeconds
) {}
