package com.qagenie.testbe.scenario.service.impl;

import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.application.service.EndpointFieldExtractor;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.entity.RiskLevel;
import com.qagenie.testbe.scenario.entity.ScenarioSource;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.service.ScenarioGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, no-external-call scenario synthesis: for every endpoint in the
 * spec version, builds a plausible positive (valid inputs, 2xx) and/or negative
 * (missing/invalid required inputs, 400) test case, rendered to the house Gherkin
 * step format (see {@link HouseScenarioGherkin}). Used whenever
 * {@code qagenie.scenario-generation.use-ai=false} - the default, since it
 * requires no API key and has no external dependency or cost.
 */
@Component("ruleBasedScenarioGenerator")
@RequiredArgsConstructor
public class RuleBasedScenarioGenerator implements ScenarioGenerator {

    private final EndpointFieldExtractor endpointFieldExtractor;

    @Override
    public List<TestScenario> generate(Application application, SpecVersion specVersion, List<ApiEndpoint> endpoints, ScenarioGenerationType type, String prompt) {
        boolean wantsPositive = type == ScenarioGenerationType.POSITIVE || type == ScenarioGenerationType.POSITIVE_NEGATIVE;
        boolean wantsNegative = type == ScenarioGenerationType.NEGATIVE || type == ScenarioGenerationType.POSITIVE_NEGATIVE;

        List<TestScenario> scenarios = new ArrayList<>();
        for (ApiEndpoint endpoint : endpoints) {
            if (wantsPositive) scenarios.add(buildScenario(application, specVersion, endpoint, true));
            if (wantsNegative) scenarios.add(buildScenario(application, specVersion, endpoint, false));
        }
        return scenarios;
    }

    private TestScenario buildScenario(Application application, SpecVersion specVersion, ApiEndpoint endpoint, boolean positive) {
        HouseScenarioGherkin.Fields fields = HouseScenarioGherkin.resolveFields(endpoint, positive);

        String title = (endpoint.getSummary() != null && !endpoint.getSummary().isBlank())
                ? endpoint.getSummary()
                : endpoint.getHttpMethod() + " " + endpoint.getPath();
        String description = title + " - " + (positive ? "Positive" : "Negative");

        TestScenario scenario = new TestScenario();
        scenario.setApplication(application);
        scenario.setSpecVersion(specVersion);
        scenario.setName(description);
        scenario.setHttpMethod(endpoint.getHttpMethod());
        scenario.setEndpoint(endpoint.getPath());
        scenario.setScenarioType(positive ? ScenarioType.POSITIVE : ScenarioType.NEGATIVE);
        scenario.setSource(ScenarioSource.AI);
        scenario.setRiskLevel(RiskLevel.MEDIUM);
        scenario.setActive(true);
        scenario.setDescription(HouseScenarioGherkin.buildGherkin(application.getName(), endpoint, fields, description, positive));
        scenario.setHeaderJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "header")));
        scenario.setPathParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "path")));
        scenario.setRequestParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "query")));
        return scenario;
    }
}
