package com.qagenie.testbe.application.dto;

public record SpecDiffEntryDto(
        String changeType,
        String endpoint,
        String description
) {}
