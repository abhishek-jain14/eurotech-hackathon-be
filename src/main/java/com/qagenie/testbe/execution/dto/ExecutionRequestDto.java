package com.qagenie.testbe.execution.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@io.swagger.v3.oas.annotations.media.Schema(description = "Payload to trigger a suite execution")
public record ExecutionRequestDto(
        @NotNull Long applicationId,
        @NotNull Long environmentId,
        String suiteName,
        @NotEmpty List<Long> scenarioIds,
        Integer maxWorkers,
        Integer timeoutSeconds
) {}
