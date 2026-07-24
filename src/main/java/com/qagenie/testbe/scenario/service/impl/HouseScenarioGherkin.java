package com.qagenie.testbe.scenario.service.impl;

import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.dto.ApiParameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the house Gherkin step format shared by every scenario source (rule-based,
 * internal LLM, external AI agent) so that whatever produced a {@code TestScenario}'s
 * description, it stays executable against the fixed step vocabulary in
 * {@code execution.cucumber.steps.ApiSteps}. Extracted out of {@link RuleBasedScenarioGenerator}
 * so a second generator can reuse the exact same template without drifting out of sync
 * with it (a hand-copied second template would silently break execution the next time
 * this one changes).
 */
final class HouseScenarioGherkin {

    private HouseScenarioGherkin() {}

    /** Header/path fields become &lt;fieldName&gt; placeholders (Test Data substitutes the real value per row);
     * query fields get a concrete sample value baked in directly (no Test Data source exists for query fields yet). */
    record Fields(List<String> headerFieldNames, List<String> pathFieldNames,
                  Map<String, Object> queryParams, String resolvedResource, boolean hasRequestBody) {}

    static Fields resolveFields(ApiEndpoint endpoint, boolean positive) {
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
        return new Fields(headerFieldNames, pathFieldNames, queryParams, resolvedResource, hasRequestBody);
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
     * All placeholders are Examples substitutions filled per Test Data row at execution time
     * (see GherkinFeatureBuilder) - no "Examples:" table is emitted here.
     */
    static String buildGherkin(String applicationName, ApiEndpoint endpoint, Fields fields, String description, boolean positive) {
        StringBuilder sb = new StringBuilder();

        sb.append("@").append(sanitizeTag(applicationName))
                .append(" @").append(sanitizeTag(endpoint.getHttpMethod() + " " + endpoint.getPath()))
                .append(" @").append(positive ? "positive" : "negative").append("\n");
        sb.append("Scenario Outline: ").append(description).append("\n");

        for (String name : fields.headerFieldNames()) {
            sb.append("  Given set header parameter ").append(name).append(" to <").append(name).append(">\n");
        }
        for (Map.Entry<String, Object> query : fields.queryParams().entrySet()) {
            sb.append("  Given set query parameter ").append(query.getKey()).append(" to ").append(query.getValue()).append("\n");
        }
        if (fields.hasRequestBody()) {
            sb.append("  And the request body is <requestBody>\n");
        }
        sb.append("  And the expected error code is <errorCode>\n");
        sb.append("  And the expected error message is <errorMsg>\n");
        sb.append("  And the expected response fields are <responseFields>\n");
        sb.append("  And the expected response body is <responseJson>\n");

        sb.append("  When user send ").append(endpoint.getHttpMethod()).append(" request to ")
                .append(applicationName).append(" application, resource : ").append(fields.resolvedResource()).append("\n");
        sb.append("  Then user recieves http status code <httpStatusCode>\n");
        sb.append("  And the response should match the expected result\n");

        return sb.toString();
    }

    static String sanitizeTag(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String cleaned = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    static Object sampleValue(String type, boolean valid) {
        String normalized = type == null ? "string" : type.toLowerCase();
        return switch (normalized) {
            case "integer", "int" -> valid ? 1 : "invalid_integer";
            case "number", "float", "double" -> valid ? 1.0 : "invalid_number";
            case "boolean" -> valid ? true : "invalid_boolean";
            default -> valid ? "sample_value" : "";
        };
    }
}
