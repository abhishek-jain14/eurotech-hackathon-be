package com.qagenie.testbe.execution.dto;

import com.qagenie.testbe.execution.entity.ResultStatus;

public record ExecutionResultDto(
        Long id, Long scenarioId, String scenarioName, Long testDataId, String testDataRecordName,
        ResultStatus resultStatus, Long responseTimeMs, String errorMessage
) {}
