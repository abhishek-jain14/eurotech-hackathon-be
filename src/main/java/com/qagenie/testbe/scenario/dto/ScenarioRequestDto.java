package com.qagenie.testbe.scenario.dto;

import com.qagenie.testbe.scenario.entity.RiskLevel;
import com.qagenie.testbe.scenario.entity.ScenarioSource;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to create or update a test scenario")
public record ScenarioRequestDto(
        @NotNull Long applicationId,
        @NotBlank @Schema(example = "Successful payment with valid card") String name,
        @Schema(example = "POST") String httpMethod,
        @Schema(example = "/payments/charge") String endpoint,
        @NotNull ScenarioType scenarioType,
        @NotNull ScenarioSource source,
        RiskLevel riskLevel,
        Boolean active,
        ApiTestData apiTestData
) {}
