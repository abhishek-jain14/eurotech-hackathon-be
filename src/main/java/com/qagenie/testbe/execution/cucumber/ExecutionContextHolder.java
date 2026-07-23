package com.qagenie.testbe.execution.cucumber;

/**
 * Carries the current environment's base URL across into the Cucumber step
 * definitions ({@link com.qagenie.testbe.execution.cucumber.steps.ApiSteps}),
 * which Cucumber instantiates itself and are otherwise not Spring-managed.
 * Cucumber runs single-threaded here (no --threads option passed), so a plain
 * ThreadLocal set on the calling thread just before {@code Main.run(...)} stays
 * visible to the step definitions for that same synchronous invocation.
 */
public final class ExecutionContextHolder {

    private static final ThreadLocal<String> BASE_URL = new ThreadLocal<>();

    private ExecutionContextHolder() {}

    public static void setBaseUrl(String baseUrl) {
        BASE_URL.set(baseUrl);
    }

    public static String getBaseUrl() {
        return BASE_URL.get();
    }

    public static void clear() {
        BASE_URL.remove();
    }
}
