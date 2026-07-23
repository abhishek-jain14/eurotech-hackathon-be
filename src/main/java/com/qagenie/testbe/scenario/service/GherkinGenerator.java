package com.qagenie.testbe.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.scenario.dto.ApiTestData;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GherkinGenerator {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Converts an ApiEndpoint along with its execution context into a Gherkin/BDD Scenario string.
     *
     * @param testData Container holding the ApiEndpoint and execution metadata.
     * @return Formatted Gherkin string.
     */
    public String generateGherkin(ApiTestData testData) {
        ApiEndpoint endpoint = testData.getEndpoint();
        StringBuilder gherkin = new StringBuilder();

        // 1. Scenario Header
        String scenarioTitle = (endpoint.getSummary() != null && !endpoint.getSummary().isBlank())
                ? endpoint.getSummary()
                : endpoint.getHttpMethod() + " request to " + endpoint.getPath();

        gherkin.append("Scenario Outline: ").append(scenarioTitle).append("\n");

        // 2. Base Endpoint Path
        gherkin.append("  Given the API endpoint is \"").append(endpoint.getPath()).append("\"\n");

        // 3. Headers
        if (testData.getHeaders() != null && !testData.getHeaders().isEmpty()) {
            for (Map.Entry<String, String> header : testData.getHeaders().entrySet()) {
                gherkin.append("  And header \"").append(header.getKey())
                        .append("\" is set to \"").append(header.getValue()).append("\"\n");
            }
        }

        // 4. Path/Query Parameters
        if (testData.getPathOrQueryParams() != null && !testData.getPathOrQueryParams().isEmpty()) {
            for (Map.Entry<String, Object> param : testData.getPathOrQueryParams().entrySet()) {
                gherkin.append("  And parameter \"").append(param.getKey())
                        .append("\" is set to \"").append(param.getValue()).append("\"\n");
            }
        }

        // 5. Request Body
        if (testData.getRequestBodyValues() != null && !testData.getRequestBodyValues().isEmpty()) {
            gherkin.append("  And the request body is:\n");
            gherkin.append("    \"\"\"\n");
            gherkin.append(indentJson(toJsonString(testData.getRequestBodyValues()), "    "));
            gherkin.append("\n    \"\"\"\n");
        }

        // 6. HTTP Action
        gherkin.append("  When a ").append(endpoint.getHttpMethod()).append(" request is sent\n");

        // 7. Expected Response Status Code
        gherkin.append("  Then the response status code should be ").append(testData.getExpectedStatusCode()).append("\n");

        // 8. Expected Response Body
        if (testData.getExpectedResponseBody() != null && !testData.getExpectedResponseBody().isBlank()) {
            gherkin.append("  And the response body should match:\n");
            gherkin.append("    \"\"\"\n");
            gherkin.append(indentJson(testData.getExpectedResponseBody(), "    "));
            gherkin.append("\n    \"\"\"\n");
        }

        return gherkin.toString();
    }

    private String toJsonString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private String indentJson(String text, String indent) {
        return indent + text.replace("\n", "\n" + indent);
    }
}
