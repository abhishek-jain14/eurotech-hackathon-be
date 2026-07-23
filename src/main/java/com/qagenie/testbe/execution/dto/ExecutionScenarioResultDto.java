package com.qagenie.testbe.execution.dto;

import com.qagenie.testbe.execution.entity.ResultStatus;

public record ExecutionScenarioResultDto(
        Long id, Long scenarioId, String scenarioName, ResultStatus overallStatus,
        Integer totalTestData, Integer passedCount, Integer failedCount, Integer skippedCount,
        Long totalResponseTimeMs
) {}
