package com.qagenie.testbe.scenario.service;

import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;

import java.util.List;

public interface ScenarioGenerationService {
    /**
     * Parses the spec version's endpoints and persists generated scenarios, tagged source=AI.
     * prompt is free-text instructions used only by the AI generator (ignored by the rule-based one).
     */
    List<ScenarioResponseDto> generate(Application application, SpecVersion specVersion, ScenarioGenerationType type, String prompt);
}
