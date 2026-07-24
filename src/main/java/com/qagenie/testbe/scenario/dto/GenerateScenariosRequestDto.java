package com.qagenie.testbe.scenario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Which scenario type(s) to auto-generate for a spec version's endpoints, plus optional " +
        "free-text instructions passed to the AI generator (ignored by the rule-based generator)")
public record GenerateScenariosRequestDto(
        @NotNull ScenarioGenerationType scenarioType,
        String prompt
) {}