package com.qagenie.testbe.scenario.service;

import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;

import java.util.List;

public interface ScenarioGenerationService {
    /** Parses the spec version's endpoints and persists generated scenarios, tagged source=AI. */
    List<ScenarioResponseDto> generate(Application application, SpecVersion specVersion, ScenarioGenerationType type);
}
