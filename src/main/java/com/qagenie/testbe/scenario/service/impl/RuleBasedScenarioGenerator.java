package com.qagenie.testbe.scenario.service.impl;

import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.dto.ApiParameter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, no-external-call scenario synthesis: for every endpoint in the
 * spec version, builds a plausible positive (valid inputs, 2xx) and/or negative
 * (missing/invalid required inputs, 400) test case, rendered to the house Gherkin
 * step format (see {@link #buildGherkin}). Used whenever
 * {@code qagenie.scenario-generation.use-ai=false} - the default, since it
 * requires no API key and has no external dependency or cost.
 */
@Component("ruleBasedScenarioGenerator")
@RequiredArgsConstructor
public class RuleBasedScenarioGenerator implements ScenarioGenerator {

    private final EndpointFieldExtractor endpointFieldExtractor;

    @Override
    public List<TestScenario> generate(Application application, SpecVersion specVersion, List<ApiEndpoint> endpoints, ScenarioGenerationType type) {
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
        Map<String, Object> headerParams = new LinkedHashMap<>();
        Map<String, Object> queryParams = new LinkedHashMap<>();
        // Path params (e.g. {id}) are left exactly as-is in the resource string, NOT
        // substituted with a sample value here - Test Data supplies the actual value
        // for each {name} placeholder at execution time.
        String resolvedResource = endpoint.getPath();

        if (endpoint.getParameters() != null) {
            for (ApiParameter param : endpoint.getParameters()) {
                if (param.getName() == null) continue;
                if (!positive && !param.isRequired()) continue; // negative case targets required params only

                String in = param.getIn() == null ? "query" : param.getIn().toLowerCase();
                if ("path".equals(in)) {
                    continue; // stays as {paramName} in resolvedResource, resolved from Test Data at run time
                }

                Object value = sampleValue(param.getType(), positive);
                switch (in) {
                    case "header" -> headerParams.put(param.getName(), value);
                    default -> queryParams.put(param.getName(), value);
                }
            }
        }

        int statusCode = positive ? defaultSuccessStatus(endpoint.getHttpMethod()) : 400;
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
        scenario.setDescription(buildGherkin(application, endpoint, resolvedResource, headerParams, queryParams, statusCode, description, positive));
        scenario.setHeaderJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "header")));
        scenario.setPathParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "path")));
        scenario.setRequestParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "query")));
        return scenario;
    }

    /**
     * House step format:
     * <pre>
     * &#64;applicationName &#64;endpointName &#64;positive
     * Scenario: description
     *   Given set header parameter name to value      (one per header)
     *   Given set query parameter name to value        (one per query param)
     *   When user send HTTP_METHOD request to applicationName application, resource : resource
     *   Then user recieves http status code statusCode
     * </pre>
     */
    private String buildGherkin(Application application, ApiEndpoint endpoint, String resolvedResource,
                                 Map<String, Object> headerParams, Map<String, Object> queryParams,
                                 int statusCode, String description, boolean positive) {
        String applicationName = application.getName();
        StringBuilder sb = new StringBuilder();

        sb.append("@").append(sanitizeTag(applicationName))
                .append(" @").append(sanitizeTag(endpoint.getHttpMethod() + " " + endpoint.getPath()))
                .append(" @").append(positive ? "positive" : "negative").append("\n");
        sb.append("Scenario: ").append(description).append("\n");

        for (Map.Entry<String, Object> header : headerParams.entrySet()) {
            sb.append("  Given set header parameter ").append(header.getKey()).append(" to ").append(header.getValue()).append("\n");
        }
        for (Map.Entry<String, Object> query : queryParams.entrySet()) {
            sb.append("  Given set query parameter ").append(query.getKey()).append(" to ").append(query.getValue()).append("\n");
        }

        sb.append("  When user send ").append(endpoint.getHttpMethod()).append(" request to ")
                .append(applicationName).append(" application, resource : ").append(resolvedResource).append("\n");
        sb.append("  Then user recieves http status code ").append(statusCode).append("\n");

        return sb.toString();
    }

    private String sanitizeTag(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String cleaned = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    private int defaultSuccessStatus(String httpMethod) {
        if (httpMethod == null) return 200;
        return "POST".equalsIgnoreCase(httpMethod) ? 201 : 200;
    }

    private Object sampleValue(String type, boolean valid) {
        String normalized = type == null ? "string" : type.toLowerCase();
        return switch (normalized) {
            case "integer", "int" -> valid ? 1 : "invalid_integer";
            case "number", "float", "double" -> valid ? 1.0 : "invalid_number";
            case "boolean" -> valid ? true : "invalid_boolean";
            default -> valid ? "sample_value" : "";
        };
    }
}
