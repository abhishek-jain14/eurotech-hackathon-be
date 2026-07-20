package com.qagenie.testbe.execution.dto;

import com.qagenie.testbe.execution.entity.ResultStatus;

public record ExecutionResultDto(
        Long id, Long scenarioId, String scenarioName, ResultStatus resultStatus,
        Long responseTimeMs, String errorMessage
) {}
