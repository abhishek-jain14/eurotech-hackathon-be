package com.qagenie.testbe.scenario.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.application.service.EndpointFieldExtractor;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.scenario.dto.ScenarioGenerationType;
import com.qagenie.testbe.scenario.entity.RiskLevel;
import com.qagenie.testbe.scenario.entity.ScenarioSource;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.service.ScenarioGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real LLM-backed scenario synthesis: sends the spec version's endpoints to a
 * configured AI provider and asks it to author positive/negative test scenarios
 * as Gherkin. Used whenever {@code qagenie.scenario-generation.use-ai=true};
 * requires {@code qagenie.scenario-generation.ai.api-key} to be configured.
 */
@Component("aiScenarioGenerator")
@RequiredArgsConstructor
public class AiScenarioGenerator implements ScenarioGenerator {

    private static final Logger log = LoggerFactory.getLogger(AiScenarioGenerator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final String API_VERSION_HEADER_VALUE = "2023-06-01";

    private final EndpointFieldExtractor endpointFieldExtractor;

    @Value("${qagenie.scenario-generation.ai.api-key:}")
    private String apiKey;

    @Value("${qagenie.scenario-generation.ai.model:}")
    private String model;

    @Value("${qagenie.scenario-generation.ai.base-url:}")
    private String baseUrl;

    @Override
    public List<TestScenario> generate(Application application, SpecVersion specVersion, List<ApiEndpoint> endpoints, ScenarioGenerationType type) {
        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(
                    "AI scenario generation is enabled (qagenie.scenario-generation.use-ai=true) but is missing " +
                    "configuration. Set AI_API_KEY, AI_MODEL and AI_BASE_URL, or disable AI generation to use the " +
                    "built-in rule-based generator.",
                    "AI_CONFIG_MISSING");
        }

        String prompt = buildPrompt(application.getName(), endpoints, type);
        String responseText = callAiProvider(prompt);
        return parseScenarios(responseText, application, specVersion, endpoints, type);
    }

    private Map<String, ApiEndpoint> indexByMethodAndPath(List<ApiEndpoint> endpoints) {
        Map<String, ApiEndpoint> byKey = new HashMap<>();
        for (ApiEndpoint endpoint : endpoints) {
            byKey.put(endpointKey(endpoint.getHttpMethod(), endpoint.getPath()), endpoint);
        }
        return byKey;
    }

    private String endpointKey(String httpMethod, String path) {
        return (httpMethod == null ? "" : httpMethod.toUpperCase()) + "|" + (path == null ? "" : path);
    }

    private String buildPrompt(String applicationName, List<ApiEndpoint> endpoints, ScenarioGenerationType type) {
        String wanted = switch (type) {
            case POSITIVE -> "ONLY positive (valid input, expect a 2xx response) scenarios";
            case NEGATIVE -> "ONLY negative (invalid/missing required input, expect a 4xx response) scenarios";
            case POSITIVE_NEGATIVE -> "both one positive AND one negative scenario";
        };

        StringBuilder endpointsJson = new StringBuilder("[");
        for (int i = 0; i < endpoints.size(); i++) {
            ApiEndpoint e = endpoints.get(i);
            if (i > 0) endpointsJson.append(",");
            endpointsJson.append(toJson(Map.of(
                    "path", nullToEmpty(e.getPath()),
                    "httpMethod", nullToEmpty(e.getHttpMethod()),
                    "summary", nullToEmpty(e.getSummary()),
                    "parameters", e.getParameters() == null ? List.of() : e.getParameters(),
                    "requestBody", e.getRequestBody() == null ? Map.of() : e.getRequestBody()
            )));
        }
        endpointsJson.append("]");

        return """
                You are generating API test scenarios for the REST application "%s". For each endpoint below, generate %s.

                Endpoints (JSON array of {path, httpMethod, summary, parameters, requestBody}):
                %s

                Respond with ONLY a raw JSON array (no markdown fences, no commentary) where each element is:
                {"path": "<endpoint path>", "httpMethod": "<HTTP method>", "scenarioType": "POSITIVE" or "NEGATIVE",
                 "name": "<short scenario name>", "gherkin": "<the scenario rendered EXACTLY in the step format below, as a single string with \\n for newlines>"}

                Required step format (follow this precisely, including wording and spelling). This is a Cucumber
                Scenario Outline - header and path fields are <fieldName> placeholders (Test Data supplies the
                actual value per row at execution time, so do NOT invent a sample value for them); query fields
                get a concrete sample value directly (no Test Data source exists for query fields); do NOT emit
                an Examples: table, it is assembled separately at execution time:
                @%s @<endpointNameTag, lowercase_snake_case derived from method+path> @positive (or @negative)
                Scenario Outline: <short description>
                  Given set header parameter <name> to <name>       (one line per header field, using the SAME <name> token as a placeholder - omit if none)
                  Given set query parameter <name> to <concrete sample value>   (one line per query parameter, omit if none)
                  And the request body is <requestBody>              (only if the endpoint has a requestBody - literal token "<requestBody>", omit otherwise)
                  When user send <HTTP_METHOD> request to %s application, resource : <path, with any {pathParam} OpenAPI placeholders rewritten to Cucumber-style <pathParam> tokens, e.g. {id} becomes <id>>
                  Then user recieves http status code <200/201 for positive, 400 for negative>
                """.formatted(applicationName, wanted, endpointsJson, sanitizeTag(applicationName), applicationName);
    }

    private String sanitizeTag(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String cleaned = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    private String callAiProvider(String prompt) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

        Map<String, Object> payload = Map.of(
                "model", model,
                "max_tokens", 8192,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(TIMEOUT)
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION_HEADER_VALUE)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("AI provider returned HTTP {}: {}", response.statusCode(), response.body());
                throw new BusinessException(
                        "AI scenario generation failed: AI provider returned HTTP " + response.statusCode(),
                        "AI_GENERATION_FAILED");
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode contentArray = root.path("content");
            if (!contentArray.isArray() || contentArray.isEmpty()) {
                throw new BusinessException("AI scenario generation failed: empty response from AI provider", "AI_GENERATION_FAILED");
            }
            return contentArray.get(0).path("text").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI provider call failed", e);
            throw new BusinessException("AI scenario generation failed: " + e.getMessage(), "AI_GENERATION_FAILED");
        }
    }

    private List<TestScenario> parseScenarios(String responseText, Application application, SpecVersion specVersion,
                                               List<ApiEndpoint> endpoints, ScenarioGenerationType type) {
        String json = stripCodeFences(responseText);
        List<Map<String, Object>> items;
        try {
            items = MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Unable to parse AI response as JSON: {}", responseText, e);
            throw new BusinessException("AI scenario generation failed: model did not return valid JSON", "AI_GENERATION_FAILED");
        }

        Map<String, ApiEndpoint> endpointsByKey = indexByMethodAndPath(endpoints);

        List<TestScenario> scenarios = new ArrayList<>();
        for (Map<String, Object> item : items) {
            ScenarioType scenarioType;
            try {
                scenarioType = ScenarioType.valueOf(String.valueOf(item.get("scenarioType")).toUpperCase());
            } catch (IllegalArgumentException e) {
                continue; // skip malformed entries rather than failing the whole batch
            }
            if (!matchesRequestedType(scenarioType, type)) continue;

            String httpMethod = String.valueOf(item.getOrDefault("httpMethod", ""));
            String path = String.valueOf(item.getOrDefault("path", ""));

            TestScenario scenario = new TestScenario();
            scenario.setApplication(application);
            scenario.setSpecVersion(specVersion);
            scenario.setHttpMethod(httpMethod);
            scenario.setEndpoint(path);
            scenario.setScenarioType(scenarioType);
            scenario.setSource(ScenarioSource.AI);
            scenario.setRiskLevel(RiskLevel.MEDIUM);
            scenario.setActive(true);
            scenario.setName(String.valueOf(item.getOrDefault("name", httpMethod + " " + path)));
            scenario.setDescription(String.valueOf(item.getOrDefault("gherkin", "")));

            ApiEndpoint matchedEndpoint = endpointsByKey.get(endpointKey(httpMethod, path));
            if (matchedEndpoint != null) {
                scenario.setHeaderJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(matchedEndpoint, "header")));
                scenario.setPathParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(matchedEndpoint, "path")));
                scenario.setRequestParamJson(endpointFieldExtractor.toJson(endpointFieldExtractor.fieldsIn(matchedEndpoint, "query")));
            }
            scenarios.add(scenario);
        }

        if (scenarios.isEmpty()) {
            throw new BusinessException("AI scenario generation returned no usable scenarios", "AI_GENERATION_EMPTY");
        }
        return scenarios;
    }

    private boolean matchesRequestedType(ScenarioType actual, ScenarioGenerationType requested) {
        return switch (requested) {
            case POSITIVE -> actual == ScenarioType.POSITIVE;
            case NEGATIVE -> actual == ScenarioType.NEGATIVE;
            case POSITIVE_NEGATIVE -> true;
        };
    }

    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BusinessException("Unable to serialize request for AI scenario generation: " + e.getMessage(), "AI_GENERATION_FAILED");
        }
    }
}
