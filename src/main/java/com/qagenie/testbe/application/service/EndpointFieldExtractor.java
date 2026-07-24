package com.qagenie.testbe.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.dto.ApiParameter;
import com.qagenie.testbe.application.dto.FieldMeta;
import com.qagenie.testbe.application.entity.SpecEndpoint;
import com.qagenie.testbe.application.entity.SpecVersion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Groups an {@link ApiEndpoint}'s parameters into header/path/query field-metadata
 * buckets (name, type, mandatory) and (de)serializes them as JSON for the
 * SPEC_ENDPOINT cache and generated TEST_SCENARIO rows. Also round-trips a cached
 * {@link SpecEndpoint} row back into a full {@link ApiEndpoint} so callers that
 * used to re-parse the raw spec (fetch-endpoints, scenario generation) can read
 * from the cache instead.
 */
@Service
public class EndpointFieldExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<FieldMeta> fieldsIn(ApiEndpoint endpoint, String location) {
        List<FieldMeta> fields = new ArrayList<>();
        if (endpoint.getParameters() == null) return fields;
        for (ApiParameter param : endpoint.getParameters()) {
            if (param.getName() == null) continue;
            String in = param.getIn() == null ? "query" : param.getIn().toLowerCase();
            if (in.equals(location)) {
                fields.add(new FieldMeta(param.getName(), param.getType(), param.isRequired()));
            }
        }
        return fields;
    }

    public String toJson(List<FieldMeta> fields) {
        if (fields == null || fields.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(fields);
        } catch (Exception e) {
            return null;
        }
    }

    public List<FieldMeta> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<FieldMeta>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Shared with RuleBasedScenarioGenerator and SpecHealingService so a healed/added field gets the same sample value a fresh generation would produce. */
    public Object sampleValue(String type, boolean valid) {
        String normalized = type == null ? "string" : type.toLowerCase();
        return switch (normalized) {
            case "integer", "int" -> valid ? 1 : "invalid_integer";
            case "number", "float", "double" -> valid ? 1.0 : "invalid_number";
            case "boolean" -> valid ? true : "invalid_boolean";
            default -> valid ? "sample_value" : "";
        };
    }

    public String requestBodyToJson(Map<String, Object> requestBody) {
        if (requestBody == null || requestBody.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(requestBody);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> requestBodyFromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<ApiParameter> toApiParameters(List<FieldMeta> fields, String location) {
        List<ApiParameter> params = new ArrayList<>();
        for (FieldMeta field : fields) {
            ApiParameter p = new ApiParameter();
            p.setName(field.fieldName());
            p.setType(field.type());
            p.setRequired(field.mandatory());
            p.setIn(location);
            params.add(p);
        }
        return params;
    }

    /** Builds an unsaved SPEC_ENDPOINT cache row from a freshly parsed endpoint. */
    public SpecEndpoint toCacheEntity(SpecVersion specVersion, ApiEndpoint endpoint) {
        SpecEndpoint cached = new SpecEndpoint();
        cached.setSpecVersion(specVersion);
        cached.setPath(endpoint.getPath());
        cached.setHttpMethod(endpoint.getHttpMethod());
        cached.setSummary(endpoint.getSummary());
        cached.setHeaderJson(toJson(fieldsIn(endpoint, "header")));
        cached.setPathParamJson(toJson(fieldsIn(endpoint, "path")));
        cached.setRequestParamJson(toJson(fieldsIn(endpoint, "query")));
        cached.setRequestBodyJson(requestBodyToJson(endpoint.getRequestBody()));
        return cached;
    }

    /** Reconstructs a full ApiEndpoint from a cached SPEC_ENDPOINT row - no data loss vs. live parsing. */
    public ApiEndpoint fromCache(SpecEndpoint cached) {
        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setPath(cached.getPath());
        endpoint.setHttpMethod(cached.getHttpMethod());
        endpoint.setSummary(cached.getSummary());

        List<ApiParameter> parameters = new ArrayList<>();
        parameters.addAll(toApiParameters(fromJson(cached.getHeaderJson()), "header"));
        parameters.addAll(toApiParameters(fromJson(cached.getPathParamJson()), "path"));
        parameters.addAll(toApiParameters(fromJson(cached.getRequestParamJson()), "query"));
        endpoint.setParameters(parameters);
        endpoint.setRequestBody(requestBodyFromJson(cached.getRequestBodyJson()));
        return endpoint;
    }
}
