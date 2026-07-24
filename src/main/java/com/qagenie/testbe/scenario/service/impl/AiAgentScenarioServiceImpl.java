package com.qagenie.testbe.scenario.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qagenie.testbe.application.dto.ApiEndpoint;
import com.qagenie.testbe.application.entity.Application;
import com.qagenie.testbe.application.entity.SpecVersion;
import com.qagenie.testbe.common.exception.BusinessException;
import com.qagenie.testbe.scenario.entity.RiskLevel;
import com.qagenie.testbe.scenario.entity.ScenarioSource;
import com.qagenie.testbe.scenario.entity.ScenarioType;
import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.scenario.service.AiAgentScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the external AI agent (a separately-hosted Cloud Run service - NOT the
 * qagenie.scenario-generation.ai.* internal-LLM path used by AiScenarioGenerator) with
 * the raw spec file and parses its response into {@link TestScenario} rows.
 * <p>
 * The agent's own Gherkin wording doesn't match {@code ApiSteps}' step vocabulary
 * (e.g. "user sends"/"receives" vs. the house "user send"/"recieves", "resource:" vs.
 * "resource :"), so its literal text is never persisted as-is. Instead, only the
 * (HTTP method, path, positive/negative) decision is trusted from each returned block;
 * the actual Gherkin is rebuilt from the endpoint's real parameter metadata via
 * {@link HouseScenarioGherkin} - identical to {@code RuleBasedScenarioGenerator} - so
 * generated scenarios are guaranteed executable regardless of how the agent phrases things.
 * <p>
 * Best-effort parsing, same philosophy as {@code SpecDiffService}: each test case comes
 * back wrapped in literal double-quote characters with no escaping guarantee, so a block
 * whose own content happens to contain a literal quote (e.g. a request-body sample) could
 * misparse. A block that can't be parsed, or doesn't match any known endpoint, is skipped
 * rather than failing the whole batch.
 */
@Service
public class AiAgentScenarioServiceImpl implements AiAgentScenarioService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentScenarioServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private static final Pattern BLOCK_PATTERN = Pattern.compile("\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern WHEN_LINE_PATTERN = Pattern.compile(
            "(?im)^\\s*When\\s+user\\s+sends?\\s+(\\S+)\\s+request\\s+to\\s+.+?\\s+application,\\s*resource\\s*:\\s*(.+?)\\s*$");

    @Value("${qagenie.scenario-generation.ai-agent.url}")
    private String agentUrl;

    @Override
    public List<TestScenario> generate(Application application, SpecVersion specVersion, List<ApiEndpoint> endpoints,
                                        byte[] specFileBytes, String fileName) {
        Map<String, ApiEndpoint> endpointsByKey = indexByMethodAndPath(endpoints);
        String testCasesText = callAgent(specFileBytes, fileName);

        List<TestScenario> scenarios = new ArrayList<>();
        Matcher blockMatcher = BLOCK_PATTERN.matcher(testCasesText);
        while (blockMatcher.find()) {
            ParsedBlock parsed = parseBlock(blockMatcher.group(1));
            if (parsed == null) continue;

            ApiEndpoint endpoint = endpointsByKey.get(endpointKey(parsed.httpMethod(), parsed.path()));
            if (endpoint == null) {
                log.warn("AI agent referenced {} {} which doesn't match any parsed endpoint - skipping",
                        parsed.httpMethod(), parsed.path());
                continue;
            }
            scenarios.add(buildScenario(application, specVersion, endpoint, parsed.positive()));
        }

        if (scenarios.isEmpty()) {
            throw new BusinessException("AI agent returned no scenarios matching this spec's endpoints", "AI_AGENT_GENERATION_EMPTY");
        }
        log.info("AI agent generated {} scenarios for application id={}", scenarios.size(), application.getId());
        return scenarios;
    }

    private TestScenario buildScenario(Application application, SpecVersion specVersion, ApiEndpoint endpoint, boolean positive) {
        HouseScenarioGherkin.Fields fields = HouseScenarioGherkin.resolveFields(endpoint, positive);
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
        scenario.setDescription(HouseScenarioGherkin.buildGherkin(application.getName(), endpoint, fields, description, positive));
        return scenario;
    }

    private record ParsedBlock(String httpMethod, String path, boolean positive) {}

    private ParsedBlock parseBlock(String block) {
        Matcher whenMatcher = WHEN_LINE_PATTERN.matcher(block);
        if (!whenMatcher.find()) {
            log.warn("Unable to find a 'When user send(s) ...' line in an AI agent test case - skipping");
            return null;
        }
        boolean positive = !firstLine(block).toLowerCase().contains("@negative");
        return new ParsedBlock(whenMatcher.group(1).toUpperCase(), whenMatcher.group(2).trim(), positive);
    }

    private String firstLine(String block) {
        int newlineIdx = block.indexOf('\n');
        return newlineIdx >= 0 ? block.substring(0, newlineIdx) : block;
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

    private String callAgent(byte[] specFileBytes, String fileName) {
        String boundary = "----qagenie-" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, specFileBytes, fileName);

        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(agentUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("AI agent returned HTTP {}: {}", response.statusCode(), response.body());
                throw new BusinessException("AI agent scenario generation failed: HTTP " + response.statusCode(), "AI_AGENT_GENERATION_FAILED");
            }
            JsonNode root = MAPPER.readTree(response.body());
            if (!"success".equalsIgnoreCase(root.path("status").asText())) {
                throw new BusinessException(
                        "AI agent scenario generation failed: " + root.path("message").asText("unknown error"),
                        "AI_AGENT_GENERATION_FAILED");
            }
            String testCases = root.path("test_cases").asText(null);
            if (testCases == null || testCases.isBlank()) {
                throw new BusinessException("AI agent returned no test cases", "AI_AGENT_GENERATION_EMPTY");
            }
            return testCases;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI agent call failed", e);
            throw new BusinessException("AI agent scenario generation failed: " + e.getMessage(), "AI_AGENT_GENERATION_FAILED");
        }
    }

    /** Hand-built multipart/form-data body - java.net.http.HttpClient has no built-in multipart support. */
    private byte[] buildMultipartBody(String boundary, byte[] fileBytes, String fileName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Unable to build request for AI agent scenario generation", "AI_AGENT_GENERATION_FAILED");
        }
    }
}
