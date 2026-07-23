package com.qagenie.testbe.application.dto;

import java.util.List;

/**
 * Result of an auto-fetch attempt. When the fetched content hashes the same
 * as the current version, changed=false and message is "Swagger file is
 * latest" - no version row, no diff. Otherwise changed=true and
 * endpointDiffs carries the per-endpoint, field-level old/new breakdown.
 */
public record SpecFetchResultDto(
        boolean changed,
        String message,
        ApplicationResponseDto application,
        SpecVersionResponseDto version,
        List<EndpointFieldDiffDto> endpointDiffs
) {}
