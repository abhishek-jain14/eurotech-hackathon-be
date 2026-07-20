package com.qagenie.testbe.testflow.dto;

import java.time.Instant;
import java.util.List;

public record TestFlowResponseDto(
        Long id, Long applicationId, String name, String description, boolean active,
        List<TestFlowStepDto> steps, Instant createdAt, Instant updatedAt
) {}
