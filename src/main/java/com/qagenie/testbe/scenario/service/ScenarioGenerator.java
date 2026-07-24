package com.qagenie.testbe.scenario.service;

import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.entity.TestScenario;

import java.util.List;

/**
 * Strategy for synthesizing {@link TestScenario} entities from a spec version's
 * parsed endpoints. Two implementations exist - {@code RuleBasedScenarioGenerator}
 * (deterministic, no external call) and {@code AiScenarioGenerator} (real LLM call) -
 * selected at runtime by {@code qagenie.scenario-generation.use-ai}.
 */
public interface ScenarioGenerator {
    /** prompt is free-text instructions from the user - only AiScenarioGenerator uses it, RuleBasedScenarioGenerator ignores it. */
    List<TestScenario> generate(Application application, SpecVersion specVersion, List<ApiEndpoint> endpoints, ScenarioGenerationType type, String prompt);
}
