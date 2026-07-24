package com.qagenie.testbe.scenario.service;

import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.scenario.entity.TestScenario;

import java.util.List;

/**
 * Wraps the external, separately-hosted "AI agent" (a Cloud Run service, not the
 * internal-LLM path behind {@code qagenie.scenario-generation.use-ai}/AiScenarioGenerator)
 * that a user can opt into from the Onboarding spec fetch/upload screen. Given the raw
 * spec file, it decides which endpoints deserve positive/negative test scenarios; the
 * resulting {@link TestScenario} rows are still rendered in the house Gherkin format
 * (see {@code HouseScenarioGherkin}) so they stay executable regardless of how the agent
 * phrases its own output.
 */
public interface AiAgentScenarioService {
    List<TestScenario> generate(Application application, SpecVersion specVersion, List<ApiEndpoint> endpoints,
                                 byte[] specFileBytes, String fileName);
}
