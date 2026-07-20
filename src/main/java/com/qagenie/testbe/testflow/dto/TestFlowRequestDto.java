package com.qagenie.testbe.testflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

public record TestFlowRequestDto(
        @NotNull Long applicationId,
        @NotBlank String name,
        String description,
        Boolean active,
        @NotEmpty @Valid List<TestFlowStepDto> steps
) {}
