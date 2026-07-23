package com.qagenie.testbe.execution.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.core.cli.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a generated .feature file to disk and runs it through Cucumber's
 * programmatic CLI entrypoint against the {@code ApiSteps} glue, then parses
 * Cucumber's own JSON report to get one result per Examples row (in row order).
 */
@Component
public class CucumberFeatureRunner {

    private static final Logger log = LoggerFactory.getLogger(CucumberFeatureRunner.class);
    private static final String GLUE_PACKAGE = "com.qagenie.testbe.execution.cucumber.steps";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${qagenie.execution.feature-dir}")
    private String featureDir;

    public record StepResult(boolean passed, long durationMs, String errorMessage,
                              ExecutionContextHolder.CallLog callLog) {}

    public List<StepResult> run(String featureText, String baseUrl) {
        Path dir = Path.of(featureDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create execution feature directory " + dir, e);
        }

        String stamp = String.valueOf(Instant.now().toEpochMilli()) + "-" + Math.abs(featureText.hashCode());
        Path featureFile = dir.resolve("run-" + stamp + ".feature");
        Path reportFile = dir.resolve("run-" + stamp + ".json");

        try {
            Files.writeString(featureFile, featureText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write feature file " + featureFile, e);
        }

        String[] argv = {
                featureFile.toAbsolutePath().toString(),
                "--glue", GLUE_PACKAGE,
                "--plugin", "json:" + reportFile.toAbsolutePath(),
                "--monochrome"
        };

        List<ExecutionContextHolder.CallLog> callLogs;
        ExecutionContextHolder.setBaseUrl(baseUrl);
        try {
            byte exitStatus = Main.run(argv, Thread.currentThread().getContextClassLoader());
            log.info("Cucumber run exit status={} feature={}", exitStatus, featureFile);
        } catch (Exception e) {
            log.error("Cucumber execution failed for feature {}", featureFile, e);
            throw new IllegalStateException("Cucumber execution failed: " + e.getMessage(), e);
        } finally {
            callLogs = new ArrayList<>(ExecutionContextHolder.getCallLogs());
            ExecutionContextHolder.clear();
        }

        return zipWithCallLogs(parseReport(reportFile), callLogs);
    }

    /** Cucumber runs Examples rows single-threaded, in order, so the Nth parsed
     * report result corresponds to the Nth CallLog ApiSteps recorded. */
    private List<StepResult> zipWithCallLogs(List<StepResult> parsed, List<ExecutionContextHolder.CallLog> callLogs) {
        List<StepResult> combined = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            StepResult base = parsed.get(i);
            ExecutionContextHolder.CallLog callLog = i < callLogs.size() ? callLogs.get(i) : null;
            combined.add(new StepResult(base.passed(), base.durationMs(), base.errorMessage(), callLog));
        }
        return combined;
    }

    private List<StepResult> parseReport(Path reportFile) {
        List<StepResult> results = new ArrayList<>();
        if (!Files.exists(reportFile)) {
            log.warn("Cucumber JSON report not found at {}", reportFile);
            return results;
        }
        try {
            JsonNode features = MAPPER.readTree(Files.readString(reportFile, StandardCharsets.UTF_8));
            for (JsonNode feature : features) {
                for (JsonNode element : feature.path("elements")) {
                    if (!"scenario".equals(element.path("type").asText())) continue;
                    results.add(evaluateElement(element));
                }
            }
        } catch (IOException e) {
            log.error("Unable to parse Cucumber JSON report {}", reportFile, e);
        }
        return results;
    }

    private StepResult evaluateElement(JsonNode element) {
        long durationNanos = 0;
        String firstError = null;
        boolean anyFailed = false;
        boolean anyUndefined = false;

        for (JsonNode step : element.path("steps")) {
            JsonNode result = step.path("result");
            String status = result.path("status").asText("");
            durationNanos += result.path("duration").asLong(0);
            if ("failed".equals(status)) {
                anyFailed = true;
                if (firstError == null) firstError = result.path("error_message").asText(null);
            } else if ("undefined".equals(status) || "pending".equals(status) || "ambiguous".equals(status)) {
                anyUndefined = true;
            }
        }

        long durationMs = durationNanos / 1_000_000;
        if (anyFailed) return new StepResult(false, durationMs, firstError != null ? firstError : "Step failed", null);
        if (anyUndefined) return new StepResult(false, durationMs, "Cucumber step undefined/pending - check step definitions", null);
        return new StepResult(true, durationMs, null, null);
    }
}
