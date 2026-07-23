package com.qagenie.testbe.execution.dto;

import com.qagenie.testbe.execution.entity.ExecutionStatus;
import java.time.Instant;
import java.util.List;

public record ExecutionRunResponseDto(
        Long id, Long applicationId, Long environmentId, String environmentName, String suiteName,
        ExecutionStatus status, Instant startedAt, Instant completedAt,
        Integer totalScenarios, Integer passedCount, Integer failedCount,
        List<ExecutionScenarioResultDto> scenarioResults,
        List<ExecutionResultDto> results
) {}
