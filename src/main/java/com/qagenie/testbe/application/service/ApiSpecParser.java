package com.qagenie.testbe.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.dto.ApiParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApiSpecParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parses refactored OpenAPI JSON string and directly returns a List of ApiEndpoints.
     *
     * @param jsonInput The raw OpenAPI JSON content.
     * @return List of parsed ApiEndpoint objects.
     * @throws JsonProcessingException If the input JSON is invalid.
     */
    public List<ApiEndpoint> parseApiEndpoints(String jsonInput) throws JsonProcessingException {
        List<ApiEndpoint> endpointsList = new ArrayList<>();
        JsonNode root = mapper.readTree(jsonInput);
        JsonNode pathsNode = root.path("paths");

        if (pathsNode.isObject()) {
            pathsNode.fields().forEachRemaining(pathEntry -> {
                String path = pathEntry.getKey();
                JsonNode methodsNode = pathEntry.getValue();

                if (methodsNode.isObject()) {
                    methodsNode.fields().forEachRemaining(methodEntry -> {
                        String httpMethod = methodEntry.getKey();
                        JsonNode detailsNode = methodEntry.getValue();

                        ApiEndpoint endpoint = new ApiEndpoint();
                        endpoint.setPath(path);
                        endpoint.setHttpMethod(httpMethod.toUpperCase());
                        endpoint.setSummary(detailsNode.path("summary").asText(""));

                        // Parse Parameters
                        if (detailsNode.has("parameters")) {
                            endpoint.setParameters(parseParameters(detailsNode.get("parameters")));
                        }

                        // Parse Request Body
                        if (detailsNode.has("requestBody")) {
                            Map<String, Object> reqBody = mapper.convertValue(
                                    detailsNode.get("requestBody"),
                                    new TypeReference<Map<String, Object>>() {}
                            );
                            endpoint.setRequestBody(reqBody);
                        }

                        // Parse Responses
                        if (detailsNode.has("responses")) {
                            Map<String, Object> responses = mapper.convertValue(
                                    detailsNode.get("responses"),
                                    new TypeReference<Map<String, Object>>() {}
                            );
                            endpoint.setResponses(responses);
                        }

                        endpointsList.add(endpoint);
                    });
                }
            });
        }

        return endpointsList;
    }

    /**
     * Builds ApiParameter from each raw parameter node instead of a blind Jackson
     * convertValue, for two reasons: (1) OpenAPI 3 parameter objects carry extra fields
     * ApiParameter doesn't model (schema, example, description, style...) which would
     * otherwise fail deserialization outright, and (2) OpenAPI 3 nests the type under
     * "schema.type" rather than the Swagger 2.0 top-level "type" - both are handled here
     * so ApiParameter.type is populated correctly regardless of spec version.
     */
    private List<ApiParameter> parseParameters(JsonNode parametersNode) {
        List<ApiParameter> params = new ArrayList<>();
        for (JsonNode paramNode : parametersNode) {
            ApiParameter param = new ApiParameter();
            param.setName(paramNode.path("name").asText(null));
            param.setIn(paramNode.path("in").asText(null));
            param.setRequired(paramNode.path("required").asBoolean(false));
            String type = paramNode.path("schema").path("type").asText(null);
            if (type == null) type = paramNode.path("type").asText(null);
            param.setType(type);
            params.add(param);
        }
        return params;
    }
}