package com.qagenie.testbe.execution.cucumber;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries the current environment's base URL across into the Cucumber step
 * definitions ({@link com.qagenie.testbe.execution.cucumber.steps.ApiSteps}),
 * which Cucumber instantiates itself and are otherwise not Spring-managed.
 * Cucumber runs single-threaded here (no --threads option passed), so a plain
 * ThreadLocal set on the calling thread just before {@code Main.run(...)} stays
 * visible to the step definitions for that same synchronous invocation.
 *
 * Also doubles as the channel {@link com.qagenie.testbe.execution.cucumber.steps.ApiSteps}
 * uses to report the full request/response it made back to {@link CucumberFeatureRunner}
 * once Cucumber returns - one {@link CallLog} per Examples row, recorded in the same
 * (single-threaded, in order) sequence Cucumber runs them in.
 */
public final class ExecutionContextHolder {

    private static final ThreadLocal<String> BASE_URL = new ThreadLocal<>();
    private static final ThreadLocal<List<CallLog>> CALL_LOGS = ThreadLocal.withInitial(ArrayList::new);

    private ExecutionContextHolder() {}

    public record CallLog(
            String requestMethod, String requestUrl, String requestHeadersJson, String requestBody,
            Integer responseStatus, String responseHeadersJson, String responseBody
    ) {}

    public static void setBaseUrl(String baseUrl) {
        BASE_URL.set(baseUrl);
    }

    public static String getBaseUrl() {
        return BASE_URL.get();
    }

    public static void recordCall(CallLog callLog) {
        CALL_LOGS.get().add(callLog);
    }

    public static List<CallLog> getCallLogs() {
        return CALL_LOGS.get();
    }

    public static void clear() {
        BASE_URL.remove();
        CALL_LOGS.remove();
    }
}
