package com.qagenie.testbe.envvariable.dto;

import java.time.Instant;

public record EnvironmentVariableResponseDto(
        Long id,
        Long projectId,
        String name,
        String value,
        Instant createdAt,
        Instant updatedAt
) {}