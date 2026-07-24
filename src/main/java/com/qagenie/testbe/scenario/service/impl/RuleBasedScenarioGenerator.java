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
        // Header and path fields become <fieldName> Scenario Outline placeholders -
        // Test Data supplies the actual value per Examples row at execution time.
        // Query fields have no Test Data capture mechanism (see TestDataPage), so they
        // keep a concrete generated sample value, same as before.
        List<String> headerFieldNames = new ArrayList<>();
        List<String> pathFieldNames = new ArrayList<>();
        Map<String, Object> queryParams = new LinkedHashMap<>();
        String resolvedResource = endpoint.getPath();

        if (endpoint.getParameters() != null) {
            for (ApiParameter param : endpoint.getParameters()) {
                if (param.getName() == null) continue;
                if (!positive && !param.isRequired()) continue; // negative case targets required fields only

                String in = param.getIn() == null ? "query" : param.getIn().toLowerCase();
                switch (in) {
                    case "header" -> headerFieldNames.add(param.getName());
                    case "path" -> {
                        pathFieldNames.add(param.getName());
                        resolvedResource = resolvedResource.replace("{" + param.getName() + "}", "<" + param.getName() + ">");
                    }
                    default -> queryParams.put(param.getName(), sampleValue(param.getType(), positive));
                }
            }
        }

        boolean hasRequestBody = endpoint.getRequestBody() != null && !endpoint.getRequestBody().isEmpty();
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
        scenario.setDescription(buildGherkin(application, endpoint, resolvedResource, headerFieldNames, queryParams, hasRequestBody, description, positive));
        scenario.setHeaderJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "header")));
        scenario.setPathParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "path")));
        scenario.setRequestParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(endpoint, "query")));
        return scenario;
    }

    /**
     * House step format:
     * <pre>
     * &#64;applicationName &#64;endpointName &#64;positive
     * Scenario Outline: description
     *   Given set header parameter name to &lt;name&gt;         (one per header field - substituted from Test Data)
     *   Given set query parameter name to value          (one per query param - concrete sample value)
     *   And the request body is &lt;requestBody&gt;              (only when the endpoint has a request body)
     *   And the expected error code is &lt;errorCode&gt;
     *   And the expected error message is &lt;errorMsg&gt;
     *   And the expected response fields are &lt;responseFields&gt;
     *   And the expected response body is &lt;responseJson&gt;
     *   When user send HTTP_METHOD request to applicationName application, resource : resource
     *   Then user recieves http status code &lt;httpStatusCode&gt;
     *   And the response should match the expected result
     * </pre>
     * &lt;httpStatusCode&gt;/&lt;errorCode&gt;/&lt;errorMsg&gt;/&lt;responseFields&gt;/&lt;responseJson&gt; are Examples
     * placeholders substituted per row from whatever Test Data is linked to this scenario at execution
     * time (see GherkinFeatureBuilder) - a row that leaves one unset falls back to a sensible default
     * (e.g. 400 for negative, 200/201 for positive) or asserts nothing for that value. No "Examples:"
     * table is emitted here - it's assembled at execution time.
     */
    private String buildGherkin(Application application, ApiEndpoint endpoint, String resolvedResource,
                                 List<String> headerFieldNames, Map<String, Object> queryParams, boolean hasRequestBody,
                                 String description, boolean positive) {
        String applicationName = application.getName();
        StringBuilder sb = new StringBuilder();

        sb.append("@").append(sanitizeTag(applicationName))
                .append(" @").append(sanitizeTag(endpoint.getHttpMethod() + " " + endpoint.getPath()))
                .append(" @").append(positive ? "positive" : "negative").append("\n");
        sb.append("Scenario Outline: ").append(description).append("\n");

        for (String name : headerFieldNames) {
            sb.append("  Given set header parameter ").append(name).append(" to <").append(name).append(">\n");
        }
        for (Map.Entry<String, Object> query : queryParams.entrySet()) {
            sb.append("  Given set query parameter ").append(query.getKey()).append(" to ").append(query.getValue()).append("\n");
        }
        if (hasRequestBody) {
            sb.append("  And the request body is <requestBody>\n");
        }
        sb.append("  And the expected error code is <errorCode>\n");
        sb.append("  And the expected error message is <errorMsg>\n");
        sb.append("  And the expected response fields are <responseFields>\n");
        sb.append("  And the expected response body is <responseJson>\n");

        sb.append("  When user send ").append(endpoint.getHttpMethod()).append(" request to ")
                .append(applicationName).append(" application, resource : ").append(resolvedResource).append("\n");
        sb.append("  Then user recieves http status code <httpStatusCode>\n");
        sb.append("  And the response should match the expected result\n");

        return sb.toString();
    }

    private String sanitizeTag(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String cleaned = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
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
