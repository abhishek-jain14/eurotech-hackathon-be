package com.qagenie.testbe.application.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class SwaggerCondenser {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final List<String> VALID_METHODS = Arrays.asList(
            "get", "post", "put", "delete", "patch"
    );

    /**
     * Accepts a raw Swagger/OpenAPI JSON String and returns the condensed JSON String.
     *
     * @param jsonInput The raw OpenAPI JSON content.
     * @return The condensed OpenAPI JSON string.
     * @throws JsonProcessingException If the input is not valid JSON.
     */
    public String condenseSwaggerJson(String jsonInput) throws JsonProcessingException {
        JsonNode spec = mapper.readTree(jsonInput);
        ObjectNode condensedPaths = mapper.createObjectNode();

        // Iterate through endpoints
        JsonNode paths = spec.path("paths");
        if (paths.isObject()) {
            paths.fields().forEachRemaining(pathEntry -> {
                String path = pathEntry.getKey();
                JsonNode methods = pathEntry.getValue();

                if (methods.isObject()) {
                    ObjectNode cleanedMethods = mapper.createObjectNode();

                    methods.fields().forEachRemaining(methodEntry -> {
                        String method = methodEntry.getKey();
                        JsonNode details = methodEntry.getValue();

                        if (!VALID_METHODS.contains(method.toLowerCase())) {
                            return;
                        }

                        ObjectNode cleanedOperation = mapper.createObjectNode();
                        cleanedOperation.put("summary", details.path("summary").asText(""));

                        // 1. Clean Path/Query Parameters
                        if (details.has("parameters") && details.get("parameters").isArray()) {
                            ArrayNode cleanedParams = mapper.createArrayNode();
                            for (JsonNode p : details.get("parameters")) {
                                ObjectNode paramObj = mapper.createObjectNode();
                                paramObj.put("name", p.path("name").asText(null));
                                paramObj.put("in", p.path("in").asText(null));
                                paramObj.put("required", p.path("required").asBoolean(false));

                                JsonNode paramSchema = p.path("schema");
                                String type = paramSchema.has("type")
                                        ? paramSchema.get("type").asText()
                                        : p.path("type").asText("string");
                                paramObj.put("type", type);

                                cleanedParams.add(paramObj);
                            }
                            cleanedOperation.set("parameters", cleanedParams);
                        }

                        // 2. Clean Request Body
                        if (details.has("requestBody")) {
                            JsonNode content = details.path("requestBody").path("content");
                            JsonNode jsonMedia = content.path("application/json");
                            if (jsonMedia.has("schema")) {
                                cleanedOperation.set("requestBody", simplifySchema(jsonMedia.get("schema"), spec, 0));
                            }
                        }

                        // 3. Clean Response Status Codes
                        if (details.has("responses") && details.get("responses").isObject()) {
                            ObjectNode condensedResponses = mapper.createObjectNode();
                            details.get("responses").fields().forEachRemaining(respEntry -> {
                                String code = respEntry.getKey();
                                JsonNode resp = respEntry.getValue();

                                ObjectNode respObj = mapper.createObjectNode();
                                JsonNode jsonMedia = resp.path("content").path("application/json");
                                if (jsonMedia.has("schema")) {
                                    respObj.set("schema", simplifySchema(jsonMedia.get("schema"), spec, 0));
                                }

                                condensedResponses.set(code, respObj);
                            });
                            cleanedOperation.set("responses", condensedResponses);
                        }

                        cleanedMethods.set(method.toUpperCase(), cleanedOperation);
                    });

                    condensedPaths.set(path, cleanedMethods);
                }
            });
        }

        // Build output structure
        ObjectNode minimalSwagger = mapper.createObjectNode();
        minimalSwagger.put("title", spec.path("info").path("title").asText("API Spec"));
        minimalSwagger.put("version", spec.path("info").path("version").asText("1.0"));
        minimalSwagger.set("paths", condensedPaths);

        return mapper.writeValueAsString(minimalSwagger);
    }

    /**
     * Extracts a schema block referenced by $ref (e.g., '#/components/schemas/User')
     */
    private static JsonNode resolveRef(String refPath, JsonNode fullSpec) {
        String cleanPath = refPath.replaceAll("^#/", "");
        String[] parts = cleanPath.split("/");
        JsonNode curr = fullSpec;

        for (String part : parts) {
            if (curr != null && curr.isObject() && curr.has(part)) {
                curr = curr.get(part);
            } else {
                return null;
            }
        }
        return curr;
    }

    /**
     * Recursively simplifies schema definitions and expands $ref pointers up to 3 levels deep.
     */
    private static JsonNode simplifySchema(JsonNode schema, JsonNode fullSpec, int depth) {
        if (depth > 3 || schema == null || !schema.isObject()) {
            return schema;
        }

        // Resolve $ref pointers
        if (schema.has("$ref")) {
            JsonNode resolved = resolveRef(schema.get("$ref").asText(), fullSpec);
            if (resolved != null) {
                return simplifySchema(resolved, fullSpec, depth + 1);
            }
            ObjectNode fallback = mapper.createObjectNode();
            fallback.put("type", "object");
            return fallback;
        }

        ObjectNode cleanS = mapper.createObjectNode();

        if (schema.has("type")) {
            cleanS.set("type", schema.get("type"));
        }
        if (schema.has("required")) {
            cleanS.set("required", schema.get("required"));
        }
        if (schema.has("enum")) {
            cleanS.set("enum", schema.get("enum"));
        }

        // Handle object properties
        if (schema.has("properties")) {
            ObjectNode propertiesNode = mapper.createObjectNode();
            schema.get("properties").fields().forEachRemaining(entry -> {
                propertiesNode.set(entry.getKey(), simplifySchema(entry.getValue(), fullSpec, depth + 1));
            });
            cleanS.set("properties", propertiesNode);
        }

        // Handle arrays
        if (schema.has("items")) {
            cleanS.set("items", simplifySchema(schema.get("items"), fullSpec, depth + 1));
        }

        return cleanS;
    }
}