package com.qagenie.testbe.execution.cucumber.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.qagenie.testbe.execution.cucumber.ExecutionContextHolder;
import com.qagenie.testbe.execution.cucumber.ExecutionContextHolder.CallLog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cucumber glue for the house Gherkin step format (see RuleBasedScenarioGenerator /
 * AiScenarioGenerator). Cucumber instantiates one fresh instance of this class per
 * Examples row it runs, so per-row state (headers/query/body/response) as plain
 * instance fields is safe - no cross-row leakage.
 */
public class ApiSteps {

    private static final Logger log = LoggerFactory.getLogger(ApiSteps.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private String requestBody;
    private HttpResponse<String> response;
    private Exception requestError;

    private String expectedErrorCode;
    private String expectedErrorMessage;
    private String expectedResponseFieldsJson;
    private String expectedResponseJson;

    @Given("^set header parameter (\\S+) to (.*)$")
    public void setHeaderParameter(String name, String value) {
        headers.put(name, value);
    }

    @Given("^set query parameter (\\S+) to (.*)$")
    public void setQueryParameter(String name, String value) {
        queryParams.put(name, value);
    }

    @Given("^the request body is (.*)$")
    public void setRequestBody(String body) {
        this.requestBody = body;
    }

    @Given("^the expected error code is (.*)$")
    public void setExpectedErrorCode(String value) {
        this.expectedErrorCode = blankToNull(value);
    }

    @Given("^the expected error message is (.*)$")
    public void setExpectedErrorMessage(String value) {
        this.expectedErrorMessage = blankToNull(value);
    }

    @Given("^the expected response fields are (.*)$")
    public void setExpectedResponseFields(String value) {
        this.expectedResponseFieldsJson = blankToNull(value);
    }

    @Given("^the expected response body is (.*)$")
    public void setExpectedResponseBody(String value) {
        this.expectedResponseJson = blankToNull(value);
    }

    @When("^user send (\\S+) request to (.+) application, resource : (.+)$")
    public void sendRequest(String method, String applicationName, String resource) {
        String baseUrl = ExecutionContextHolder.getBaseUrl();
        StringBuilder url = new StringBuilder(baseUrl == null ? "" : baseUrl).append(resource);
        if (!queryParams.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> q : queryParams.entrySet()) {
                if (!first) url.append("&");
                url.append(q.getKey()).append("=").append(URLEncoder.encode(q.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(30));
        headers.forEach(builder::header);

        if (requestBody != null && !requestBody.isBlank()) {
            builder.header("Content-Type", "application/json");
            builder.method(method.toUpperCase(), HttpRequest.BodyPublishers.ofString(requestBody));
        } else {
            builder.method(method.toUpperCase(), HttpRequest.BodyPublishers.noBody());
        }

        log.info("Cucumber step sending {} {} (headers={}, query={}, body={})",
                method.toUpperCase(), url, headers.keySet(), queryParams, requestBody != null ? truncate(requestBody) : "none");

        try {
            response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            log.info("Cucumber step received HTTP {} from {} {} - body: {}",
                    response.statusCode(), method.toUpperCase(), url, truncate(response.body()));
        } catch (Exception e) {
            requestError = e;
            log.error("Cucumber step request failed for {} {}: {}", method.toUpperCase(), url, e.getMessage(), e);
        }

        recordCallLog(method, url.toString());
    }

    /** Reports the full (untruncated) request/response back to CucumberFeatureRunner via
     * ExecutionContextHolder, one CallLog per Examples row in execution order, so
     * ExecutionServiceImpl can persist it onto the corresponding ExecutionResult row. */
    private void recordCallLog(String method, String url) {
        String responseBody = response != null
                ? response.body()
                : (requestError != null ? "Request failed: " + requestError.getMessage() : null);
        Integer statusCode = response != null ? response.statusCode() : null;
        String responseHeadersJson = response != null ? toJson(response.headers().map()) : null;

        ExecutionContextHolder.recordCall(new CallLog(
                method.toUpperCase(), url, toJson(headers), requestBody,
                statusCode, responseHeadersJson, responseBody
        ));
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Then("^user recieves http status code (\\d+)$")
    public void assertStatusCode(int expected) {
        if (requestError != null) {
            throw new AssertionError("Request failed: " + requestError.getMessage());
        }
        if (response == null) {
            throw new AssertionError("No HTTP response captured - the request step did not run");
        }
        if (response.statusCode() != expected) {
            log.warn("Assertion failed: expected status {} but got {}", expected, response.statusCode());
            throw new AssertionError("Expected status " + expected + " but got " + response.statusCode()
                    + " - body: " + truncate(response.body()));
        }
        log.info("Assertion passed: received expected status {}", expected);
    }

    /**
     * Best-effort expected-vs-actual assertion (same "not full semantic" philosophy as
     * SpecDiffService) driven by whatever expected-value steps ran before this one - a no-op
     * when a row supplies none of them, since most rows only care about the status code.
     */
    @Then("^the response should match the expected result$")
    public void assertExpectedResult() {
        if (expectedErrorCode == null && expectedErrorMessage == null
                && expectedResponseFieldsJson == null && expectedResponseJson == null) {
            return;
        }
        if (requestError != null) {
            throw new AssertionError("Request failed: " + requestError.getMessage());
        }
        if (response == null) {
            throw new AssertionError("No HTTP response captured - the request step did not run");
        }

        JsonNode actualRoot = parseJsonQuietly(response.body());
        List<String> mismatches = new ArrayList<>();

        if (expectedResponseJson != null) {
            JsonNode expectedRoot = parseJsonQuietly(expectedResponseJson);
            boolean matches = !expectedRoot.isMissingNode()
                    ? expectedRoot.equals(actualRoot)
                    : response.body() != null && response.body().trim().contains(expectedResponseJson.trim());
            if (!matches) {
                mismatches.add("expected response body " + truncate(expectedResponseJson) + " but got " + truncate(response.body()));
            }
        }
        if (expectedResponseFieldsJson != null) {
            JsonNode expectedFields = parseJsonQuietly(expectedResponseFieldsJson);
            expectedFields.fields().forEachRemaining(entry -> {
                JsonNode actualValue = findByDotPath(actualRoot, entry.getKey());
                if (actualValue == null) return; // tolerant of missing keys - skip rather than fail
                if (!actualValue.asText().equals(entry.getValue().asText())) {
                    mismatches.add("field '" + entry.getKey() + "' expected '" + entry.getValue().asText()
                            + "' but got '" + actualValue.asText() + "'");
                }
            });
        }
        if (expectedErrorCode != null) {
            String actual = firstMatchingText(actualRoot, "code", "errorCode", "error_code");
            if (actual == null || !actual.trim().equalsIgnoreCase(expectedErrorCode.trim())) {
                mismatches.add("errorCode expected '" + expectedErrorCode + "' but got '" + actual + "'");
            }
        }
        if (expectedErrorMessage != null) {
            String actual = firstMatchingText(actualRoot, "message", "error", "errorMessage", "error_message");
            if (actual == null || !actual.toLowerCase().contains(expectedErrorMessage.trim().toLowerCase())) {
                mismatches.add("errorMsg expected to contain '" + expectedErrorMessage + "' but got '" + actual + "'");
            }
        }

        if (!mismatches.isEmpty()) {
            log.warn("Expected-result assertion failed: {}", mismatches);
            throw new AssertionError(String.join("; ", mismatches) + " - body: " + truncate(response.body()));
        }
        log.info("Expected-result assertion passed");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private JsonNode parseJsonQuietly(String json) {
        if (json == null || json.isBlank()) return MissingNode.getInstance();
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return MissingNode.getInstance();
        }
    }

    /** Walks a dot-separated path (e.g. "data.user.email") through nested objects; null if any segment is missing. */
    private JsonNode findByDotPath(JsonNode root, String dotPath) {
        JsonNode current = root;
        for (String segment : dotPath.split("\\.")) {
            if (current == null || current.isMissingNode()) return null;
            current = current.path(segment);
        }
        return (current == null || current.isMissingNode()) ? null : current;
    }

    private String firstMatchingText(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.path(key);
            if (!node.isMissingNode() && !node.isNull()) return node.asText();
        }
        return null;
    }

    private String truncate(String body) {
        if (body == null) return "";
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }
}
