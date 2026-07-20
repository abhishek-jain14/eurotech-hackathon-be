package com.qagenie.testbe.scenario.dto;

import com.qagenie.testbe.scenario.entity.RiskLevel;
import com.qagenie.testbe.scenario.entity.ScenarioSource;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import java.time.Instant;

public record ScenarioResponseDto(
        Long id, Long applicationId, String applicationName, String name, String httpMethod, String endpoint,
        ScenarioType scenarioType, ScenarioSource source, RiskLevel riskLevel, String description,
        boolean active, Long specVersionNumber, Instant createdAt, Instant updatedAt
) {}
