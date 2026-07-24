package com.qagenie.testbe.scenario.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecEndpoint;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.application.repository.SpecEndpointRepository;
import com.qagenie.testbe.application.service.ApiSpecParser;
import com.qagenie.testbe.application.service.EndpointFieldExtractor;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.dto.ScenarioResponseDto;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.mapper.ScenarioMapper;
import com.qagenie.testbe.scenario.repository.TestScenarioRepository;
import com.qagenie.testbe.scenario.service.ScenarioGenerationService;
import com.qagenie.testbe.scenario.service.ScenarioGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Picks the rule-based or AI generator per {@code qagenie.scenario-generation.use-ai},
 * parses the target spec version's endpoints, and persists whatever the chosen
 * generator produces. Field names below intentionally match the two
 * {@link ScenarioGenerator} beans' names ("ruleBasedScenarioGenerator" /
 * "aiScenarioGenerator") so Spring resolves the by-type ambiguity by name
 * without needing an explicit @Qualifier on a Lombok-generated constructor.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScenarioGenerationServiceImpl implements ScenarioGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioGenerationServiceImpl.class);

    private final ApiSpecParser apiSpecParser;
    private final SpecEndpointRepository specEndpointRepository;
    private final EndpointFieldExtractor endpointFieldExtractor;
    private final TestScenarioRepository testScenarioRepository;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioGenerator ruleBasedScenarioGenerator;
    private final ScenarioGenerator aiScenarioGenerator;

    @Value("${qagenie.scenario-generation.use-ai:false}")
    private boolean useAi;

    @Override
    public List<ScenarioResponseDto> generate(Application application, SpecVersion specVersion, ScenarioGenerationType type, String prompt) {
        List<ApiEndpoint> endpoints = resolveEndpoints(specVersion);
        if (endpoints.isEmpty()) {
            throw new BusinessException(
                    "Spec version v" + specVersion.getVersionNumber() + " has no endpoints to generate scenarios from",
                    "NO_ENDPOINTS_FOUND");
        }

        ScenarioGenerator generator = useAi ? aiScenarioGenerator : ruleBasedScenarioGenerator;
        log.info("Generating {} scenarios for application id={} spec version v{} via {}",
                type, application.getId(), specVersion.getVersionNumber(), useAi ? "AI" : "rule-based generator");

        List<TestScenario> generated = generator.generate(application, specVersion, endpoints, type, prompt);
        List<TestScenario> saved = testScenarioRepository.saveAll(generated);
        return saved.stream().map(scenarioMapper::toResponseDto).toList();
    }

    /** Reads from the SPEC_ENDPOINT cache when available, falling back to a live parse for versions ingested before it existed. */
    private List<ApiEndpoint> resolveEndpoints(SpecVersion specVersion) {
        List<SpecEndpoint> cached = specEndpointRepository.findBySpecVersionId(specVersion.getId());
        if (!cached.isEmpty()) {
            return cached.stream().map(endpointFieldExtractor::fromCache).toList();
        }
        try {
            return apiSpecParser.parseApiEndpoints(specVersion.getContent());
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "Unable to parse endpoints from spec version v" + specVersion.getVersionNumber(), "SPEC_PARSE_ERROR");
        }
    }
}
