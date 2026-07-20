package com.qagenie.testbe.environment.dto;

import java.time.Instant;

public record EnvironmentResponseDto(
        Long id,
        Long projectId,
        String projectName,
        String envName,
        String configType,
        String baseUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
