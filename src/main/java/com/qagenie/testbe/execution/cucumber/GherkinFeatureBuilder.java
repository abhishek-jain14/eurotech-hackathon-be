package com.qagenie.testbe.execution.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.testdata.entity.TestData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the executable .feature text for a scenario: takes its stored Scenario
 * Outline (with &lt;fieldName&gt; placeholders, from RuleBasedScenarioGenerator /
 * AiScenarioGenerator) and appends an Examples: table with one row per linked
 * TestData record, columns drawn from the scenario's own header/path field lists
 * (plus a requestBody column when the Outline references one).
 */
@Component
public class GherkinFeatureBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String build(TestScenario scenario, List<TestData> testDataRows) {
        String outline = scenario.getDescription();
        if (outline == null || outline.isBlank()) {
            throw new IllegalStateException("Scenario " + scenario.getId() + " has no Gherkin description to execute");
        }

        List<String> columns = resolveColumns(scenario, outline);
        int defaultStatus = defaultStatusCode(scenario);

        StringBuilder feature = new StringBuilder();
        feature.append("Feature: ").append(scenario.getName()).append("\n\n");
        feature.append(outline);

        if (!columns.isEmpty() && !testDataRows.isEmpty()) {
            feature.append("\nExamples:\n");
            feature.append("  | ").append(String.join(" | ", columns)).append(" |\n");
            for (TestData row : testDataRows) {
                feature.append("  | ").append(String.join(" | ", rowValues(row, columns, defaultStatus))).append(" |\n");
            }
        }

        return feature.toString();
    }

    /** Mirrors the default status a scenario used to hardcode into its Outline text before <httpStatusCode>
     * became a per-row placeholder, so a TestData row that leaves httpStatusCode unset behaves like before. */
    private int defaultStatusCode(TestScenario scenario) {
        if (scenario.getScenarioType() == ScenarioType.NEGATIVE) return 400;
        return "POST".equalsIgnoreCase(scenario.getHttpMethod()) ? 201 : 200;
    }

    /** Only include a column if its <name> placeholder is actually referenced in the Outline text. */
    private List<String> resolveColumns(TestScenario scenario, String outline) {
        Set<String> columns = new LinkedHashSet<>();
        for (String name : fieldNames(scenario.getHeaderJson())) {
            if (outline.contains("<" + name + ">")) columns.add(name);
        }
        for (String name : fieldNames(scenario.getPathParamJson())) {
            if (outline.contains("<" + name + ">")) columns.add(name);
        }
        if (outline.contains("<requestBody>")) columns.add("requestBody");
        if (outline.contains("<httpStatusCode>")) columns.add("httpStatusCode");
        if (outline.contains("<errorCode>")) columns.add("errorCode");
        if (outline.contains("<errorMsg>")) columns.add("errorMsg");
        if (outline.contains("<responseFields>")) columns.add("responseFields");
        if (outline.contains("<responseJson>")) columns.add("responseJson");
        return new ArrayList<>(columns);
    }

    private List<String> fieldNames(String fieldMetaJson) {
        List<String> names = new ArrayList<>();
        if (fieldMetaJson == null || fieldMetaJson.isBlank()) return names;
        try {
            JsonNode arr = MAPPER.readTree(fieldMetaJson);
            if (arr.isArray()) {
                arr.forEach(n -> {
                    JsonNode fieldName = n.get("fieldName");
                    if (fieldName != null) names.add(fieldName.asText());
                });
            }
        } catch (Exception ignored) {
            // malformed cache entry - treat as no fields rather than failing the whole run
        }
        return names;
    }

    private List<String> rowValues(TestData row, List<String> columns, int defaultStatus) {
        JsonNode fields = parseFieldsJson(row.getFieldsJson());
        JsonNode headers = fields.path("headers");
        JsonNode pathParams = fields.path("pathParams");
        JsonNode requestBody = fields.path("requestBody");

        List<String> values = new ArrayList<>();
        for (String column : columns) {
            switch (column) {
                case "requestBody" -> values.add(compactJson(requestBody));
                case "httpStatusCode" -> values.add(row.getHttpStatusCode() != null
                        ? row.getHttpStatusCode().toString() : String.valueOf(defaultStatus));
                case "errorCode" -> values.add(cellSafe(nullToEmpty(row.getErrorCode())));
                case "errorMsg" -> values.add(cellSafe(nullToEmpty(row.getErrorMsg())));
                case "responseFields" -> values.add(cellSafe(nullToEmpty(row.getResponseFields())));
                case "responseJson" -> values.add(cellSafe(nullToEmpty(row.getResponseJson())));
                default -> {
                    if (headers.has(column)) {
                        values.add(cellSafe(headers.get(column).asText()));
                    } else if (pathParams.has(column)) {
                        values.add(cellSafe(pathParams.get(column).asText()));
                    } else {
                        values.add("");
                    }
                }
            }
        }
        return values;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private JsonNode parseFieldsJson(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) return MAPPER.createObjectNode();
        try {
            return MAPPER.readTree(fieldsJson);
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }

    private String compactJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        return node.toString(); // Jackson's default toString() is already compact/single-line
    }

    private String cellSafe(String value) {
        // Examples cells are pipe-delimited and single-line; a literal "|" or newline (e.g. from
        // pretty-printed expected responseJson/responseFields) would otherwise break the table.
        if (value == null) return "";
        return value.replace("|", "\\|").replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
    }
}
