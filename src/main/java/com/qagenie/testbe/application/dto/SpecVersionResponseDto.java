package com.qagenie.testbe.application.dto;

import java.time.Instant;

public record SpecVersionResponseDto(
        Long id,
        Long applicationId,
        Integer versionNumber,
        String fileName,
        String contentHash,
        String source,
        String status,
        Instant fetchedAt,
        Instant lastCheckedAt,
        Instant reviewedAt,
        String reviewedBy
) {}
