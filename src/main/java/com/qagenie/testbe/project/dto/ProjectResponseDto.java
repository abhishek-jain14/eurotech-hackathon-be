package com.qagenie.testbe.project.dto;

import java.time.Instant;

public record ProjectResponseDto(
        Long id,
        String name,
        String description,
        String jiraUrl,
        String specPathSuffix,
        String specAuthType,
        boolean tlsConfigured,
        Instant createdAt,
        Instant updatedAt
) {}
