package com.qagenie.testbe.application.dto;

import com.qagenie.testbe.application.entity.ApplicationStatus;
import com.qagenie.testbe.application.entity.ApplicationType;
import com.qagenie.testbe.application.entity.SpecFormat;

import java.time.Instant;

public record ApplicationResponseDto(
        Long id,
        Long projectId,
        String projectName,
        Long referenceEnvironmentId,
        String referenceEnvironmentName,
        String name,
        String description,
        ApplicationType applicationType,
        SpecFormat specFormat,
        String specFileName,
        String specSourceMode,
        String specSourceUrl,
        Integer currentSpecVersionNumber,
        boolean hasPendingSpecVersion,
        Instant specLastFetchedAt,
        boolean autoSyncEnabled,
        Integer autoSyncIntervalMinutes,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
