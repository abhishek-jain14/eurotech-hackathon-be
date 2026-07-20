package com.qagenie.testbe.testflow.dto;

import jakarta.validation.constraints.NotNull;

public record TestFlowStepDto(
        Long id,
        @NotNull Long scenarioId,
        String scenarioName,
        @NotNull Integer sequenceOrder
) {}
